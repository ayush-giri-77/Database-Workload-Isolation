package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantConcurrencyLock;
import com.workloadgovernance.database.TenantDatabaseContext;
import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.RequestStatus;
import com.workloadgovernance.model.Tenant;
import com.workloadgovernance.model.WorkloadPolicy;
import com.workloadgovernance.repository.RequestRepository;
import com.workloadgovernance.repository.TenantRepository;
import com.workloadgovernance.repository.WorkloadPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class WorkloadAdmissionService {
    private final RequestRepository requestRepository;
    private final WorkloadPolicyRepository policyRepository;
    private final TenantRepository tenantRepository;
    private final TenantDatabaseContext tenantDatabaseContext;
    private final TenantConcurrencyLock tenantConcurrencyLock;

    public WorkloadAdmissionService(
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
    public RequestStatus admit(String requestId, String tenantId) {
        tenantDatabaseContext.setTenantId(tenantId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Request not found: " + requestId));

        if (request.getStatus() != RequestStatus.CLASSIFIED) {
            throw new IllegalStateException(
                    "Only CLASSIFIED requests can be admitted");
        }

        // Serialize admissions across workload types without requiring UPDATE
        // privileges on the tenant table.
        tenantConcurrencyLock.lock(tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException(
                        "Tenant not found during admission: " + tenantId));

        RequestStatus nextStatus;
        if (!tenant.isActive()) {
            nextStatus = RequestStatus.REJECTED;
        } else {
            Optional<WorkloadPolicy> policyResult =
                    policyRepository.findByTenantIdAndWorkloadTypeForUpdate(
                            tenantId, request.getWorkloadType());

            if (policyResult.isEmpty() || !policyResult.get().isEnabled()) {
                nextStatus = RequestStatus.REJECTED;
            } else {
                WorkloadPolicy policy = policyResult.get();
                int runningForType =
                        requestRepository.countByTenantAndTypeAndStatus(
                                tenantId,
                                request.getWorkloadType(),
                                RequestStatus.RUNNING);
                int runningForTenant =
                        requestRepository.countByTenantAndStatus(
                                tenantId,
                                RequestStatus.RUNNING);

                if (runningForType < policy.getMaxConcurrentRequests()
                        && runningForTenant < tenant.getMaxConcurrency()) {
                    nextStatus = RequestStatus.RUNNING;
                } else {
                    int queued = requestRepository.countByTenantAndTypeAndStatus(
                            tenantId,
                            request.getWorkloadType(),
                            RequestStatus.QUEUED);
                    nextStatus = queued < policy.getMaxQueueSize()
                            ? RequestStatus.QUEUED
                            : RequestStatus.REJECTED;
                }
            }
        }

        requestRepository.recordAdmissionDecision(
                requestId, nextStatus, Instant.now());
        return nextStatus;
    }
}
