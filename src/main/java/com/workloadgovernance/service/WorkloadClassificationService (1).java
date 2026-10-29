package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantDatabaseContext;
import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.RequestStatus;
import com.workloadgovernance.model.WorkloadPolicy;
import com.workloadgovernance.model.WorkloadType;
import com.workloadgovernance.repository.RequestRepository;
import com.workloadgovernance.repository.WorkloadPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class WorkloadClassificationService {

    private static final Pattern JOIN_PATTERN =
            Pattern.compile("\\bjoin\\b", Pattern.CASE_INSENSITIVE);

    private final RequestRepository requestRepository;
    private final WorkloadPolicyRepository policyRepository;
    private final TenantDatabaseContext tenantDatabaseContext;

    public WorkloadClassificationService(
            RequestRepository requestRepository,
            WorkloadPolicyRepository policyRepository,
            TenantDatabaseContext tenantDatabaseContext) {
        this.requestRepository = requestRepository;
        this.policyRepository = policyRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional
    public Request classify(String tenantId, String requestId) {
        // Set tenant context before accessing the request.
        tenantDatabaseContext.setTenantId(tenantId);

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Request not found: " + requestId));

        // Ensure the request belongs to the supplied tenant.
        if (!request.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException(
                    "Request does not belong to tenant: " + tenantId
            );
        }

        if (request.getStatus() != RequestStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Only SUBMITTED requests can be classified"
            );
        }

        WorkloadType workloadType = classifySql(request.getSql());

        Optional<WorkloadPolicy> policy =
                policyRepository.findByTenantIdAndWorkloadType(
                        tenantId,
                        workloadType
                );

        RequestStatus nextStatus = policy
                .filter(WorkloadPolicy::isEnabled)
                .map(ignored -> RequestStatus.CLASSIFIED)
                .orElse(RequestStatus.REJECTED);

        requestRepository.updateClassification(
                request.getId(),
                workloadType,
                nextStatus
        );

        request.setWorkloadType(workloadType);
        request.setStatus(nextStatus);
        return request;
    }

    private WorkloadType classifySql(String sql) {
        String normalized = sql.toLowerCase();

        long joins = JOIN_PATTERN.matcher(normalized).results().count();
        boolean hasUnion = normalized.matches("(?s).*\\bunion\\b.*");
        boolean hasAggregation =
                normalized.contains("group by")
                        || normalized.matches(
                        "(?s).*\\b(count|sum|avg|min|max)\\s*\\(.*"
                );

        if (joins >= 3 || hasUnion) {
            return WorkloadType.HEAVY;
        }

        if (joins >= 1 || hasAggregation) {
            return WorkloadType.MEDIUM;
        }

        return WorkloadType.LIGHT;
    }
}