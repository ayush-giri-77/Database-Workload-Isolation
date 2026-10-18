package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantDatabaseContext;
import com.workloadgovernance.repository.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ExecutionStartRecorder {

    private final RequestRepository requestRepository;
    private final TenantDatabaseContext tenantDatabaseContext;

    public ExecutionStartRecorder(
            RequestRepository requestRepository,
            TenantDatabaseContext tenantDatabaseContext) {
        this.requestRepository = requestRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional
    public Instant startBaselineRequest(String requestId, String tenantId) {
        tenantDatabaseContext.setTenantId(tenantId);
        Instant startTime = Instant.now();
        requestRepository.startBaselineQueuedRequest(requestId, startTime);
        return startTime;
    }
}