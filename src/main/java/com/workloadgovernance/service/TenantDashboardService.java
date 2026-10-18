package com.workloadgovernance.service;

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
import java.util.List;

@Service
public class TenantDashboardService {

    private final TenantDatabaseContext tenantDatabaseContext;
    private final TenantRepository tenantRepository;
    private final WorkloadPolicyRepository policyRepository;
    private final RequestRepository requestRepository;
    private final TenantMetricsSummaryService metricsSummaryService;

    public TenantDashboardService(
            TenantDatabaseContext tenantDatabaseContext,
            TenantRepository tenantRepository,
            WorkloadPolicyRepository policyRepository,
            RequestRepository requestRepository,
            TenantMetricsSummaryService metricsSummaryService) {
        this.tenantDatabaseContext = tenantDatabaseContext;
        this.tenantRepository = tenantRepository;
        this.policyRepository = policyRepository;
        this.requestRepository = requestRepository;
        this.metricsSummaryService = metricsSummaryService;
    }

    @Transactional(readOnly = true)
    public DashboardSnapshot snapshot(String tenantId, Instant since, int requestLimit) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID must not be blank");
        }
        if (requestLimit < 1 || requestLimit > 50) {
            throw new IllegalArgumentException("Request limit must be between 1 and 50");
        }

        tenantDatabaseContext.setTenantId(tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        List<PolicyView> policies = policyRepository.findByTenantId(tenantId)
                .stream()
                .map(PolicyView::from)
                .toList();
        List<RequestView> recentRequests = requestRepository
                .findRecentByTenantId(tenantId, requestLimit)
                .stream()
                .map(RequestView::from)
                .toList();

        return new DashboardSnapshot(
                tenant.getId(),
                tenant.getName(),
                tenant.getWeight(),
                tenant.getMaxConcurrency(),
                tenant.isActive(),
                requestRepository.countByTenantAndStatus(tenantId, RequestStatus.RUNNING),
                requestRepository.countByTenantAndStatus(tenantId, RequestStatus.QUEUED),
                metricsSummaryService.summarize(tenantId, since),
                policies,
                recentRequests,
                Instant.now());
    }

    public record DashboardSnapshot(
            String tenantId,
            String tenantName,
            int weight,
            int maxConcurrency,
            boolean active,
            int runningNow,
            int queuedNow,
            TenantMetricsSummaryService.MetricsSummary summary,
            List<PolicyView> policies,
            List<RequestView> recentRequests,
            Instant updatedAt) {}

    public record PolicyView(
            String workloadType,
            int maxConcurrentRequests,
            int maxQueueSize,
            boolean enabled) {
        private static PolicyView from(WorkloadPolicy policy) {
            return new PolicyView(
                    policy.getWorkloadType().name(),
                    policy.getMaxConcurrentRequests(),
                    policy.getMaxQueueSize(),
                    policy.isEnabled());
        }
    }

    public record RequestView(
            String id,
            String workloadType,
            String status,
            Instant arrivalTime,
            Instant queueEntryTime,
            Instant executionStartTime,
            Instant completionTime) {
        private static RequestView from(Request request) {
            return new RequestView(
                    request.getId(),
                    request.getWorkloadType() == null
                            ? "UNCLASSIFIED" : request.getWorkloadType().name(),
                    request.getStatus().name(),
                    request.getArrivalTime(),
                    request.getQueueEntryTime(),
                    request.getExecutionStartTime(),
                    request.getCompletionTime());
        }
    }
}
