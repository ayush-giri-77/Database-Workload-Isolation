package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantDatabaseContext;
import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.RequestStatus;
import com.workloadgovernance.model.Tenant;
import com.workloadgovernance.repository.RequestRepository;
import com.workloadgovernance.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class WorkloadRequestService {

    private final TenantRepository tenantRepository;
    private final RequestRepository requestRepository;
    private final TenantDatabaseContext tenantDatabaseContext;

    public WorkloadRequestService(
            TenantRepository tenantRepository,
            RequestRepository requestRepository,
            TenantDatabaseContext tenantDatabaseContext) {
        this.tenantRepository = tenantRepository;
        this.requestRepository = requestRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional
    public Request submit(String tenantId, String sqlText) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID must not be blank");
        }
        if (sqlText == null || sqlText.isBlank()) {
            throw new IllegalArgumentException("SQL text must not be blank");
        }

        // Set tenant context before any tenant-scoped database lookup.
        tenantDatabaseContext.setTenantId(tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Tenant not found: " + tenantId));

        if (!tenant.isActive()) {
            throw new IllegalArgumentException("Tenant is inactive: " + tenantId);
        }

        Request request = new Request();
        request.setId(UUID.randomUUID().toString());
        request.setTenant(tenant);
        request.setSql(sqlText);
        request.setWorkloadType(null);
        request.setStatus(RequestStatus.SUBMITTED);
        request.setArrivalTime(Instant.now());

        requestRepository.save(request);
        return request;
    }
}