package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantConcurrencyLock;
import com.workloadgovernance.database.TenantDatabaseContext;
import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.RequestStatus;
import com.workloadgovernance.model.Tenant;
import com.workloadgovernance.model.WorkloadPolicy;
import com.workloadgovernance.model.WorkloadType;
import com.workloadgovernance.repository.RequestRepository;
import com.workloadgovernance.repository.TenantRepository;
import com.workloadgovernance.repository.WorkloadPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class QueuePromotionService {
    private final RequestRepository requestRepository;
    private final WorkloadPolicyRepository policyRepository;
    private final TenantRepository tenantRepository;
    private final TenantDatabaseContext tenantDatabaseContext;
    private final TenantConcurrencyLock tenantConcurrencyLock;

    public QueuePromotionService(
            RequestRepository requestRepository,
            WorkloadPolicyRepository policyRepository,
            TenantRepository tenantRepository,
            TenantDatabaseContext tenantDatabaseContext,
            TenantConcurrencyLock tenantConcurrencyLock) {
        this.requestRepository = requestRepository;
        this.policyRepository = policyRepository;
        this.tenantRepository = tenantRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
        this.tenantConcurrencyLock = tenantConcurrencyLock;
    }

    @Transactional
    public Optional<Request> claimNext(String tenantId, WorkloadType type) {
        tenantDatabaseContext.setTenantId(tenantId);

        // Admissions have already reserved slots for these RUNNING requests.
        Optional<Request> admitted =
                requestRepository.findOldestUnstartedRunningForUpdate(
                        tenantId, type);
        if (admitted.isPresent()) {
            Request request = admitted.get();
            Instant startTime = Instant.now();
            requestRepository.markExecutionStarted(request.getId(), startTime);
            request.setExecutionStartTime(startTime);
            return admitted;
        }

        // Use the same transaction-scoped tenant lock as admission.
        tenantConcurrencyLock.lock(tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant not found during queue promotion: " + tenantId));

        int runningForTenant = requestRepository.countByTenantAndStatus(
                tenantId, RequestStatus.RUNNING);
        if (runningForTenant >= tenant.getMaxConcurrency()) {
            return Optional.empty();
        }

        Optional<WorkloadPolicy> policyResult =
                policyRepository.findByTenantIdAndWorkloadTypeForUpdate(
                        tenantId, type);
        if (policyResult.isEmpty() || !policyResult.get().isEnabled()) {
            return Optional.empty();
        }

        WorkloadPolicy policy = policyResult.get();
        int runningForType = requestRepository.countByTenantAndTypeAndStatus(
                tenantId, type, RequestStatus.RUNNING);
        if (runningForType >= policy.getMaxConcurrentRequests()) {
            return Optional.empty();
        }

        Optional<Request> queued =
                requestRepository.findOldestQueuedForUpdate(tenantId, type);
        if (queued.isEmpty()) {
            return Optional.empty();
        }

        Request request = queued.get();
        Instant startTime = Instant.now();
        requestRepository.promoteQueuedToRunning(request.getId(), startTime);
        request.setStatus(RequestStatus.RUNNING);
        request.setExecutionStartTime(startTime);
        return queued;
    }

    @Transactional
    public int recoverStaleRunning(
            String tenantId, WorkloadType type, Duration maxExecutionTime) {
        if (maxExecutionTime == null
                || maxExecutionTime.isZero()
                || maxExecutionTime.isNegative()) {
            throw new IllegalArgumentException(
                    "Maximum execution time must be positive");
        }

        tenantDatabaseContext.setTenantId(tenantId);
        Instant now = Instant.now();
        return requestRepository.failStaleRunning(
                tenantId, type, now.minus(maxExecutionTime), now);
    }
}
