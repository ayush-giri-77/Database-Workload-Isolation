package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantDatabaseContext;
import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.RequestStatus;
import com.workloadgovernance.repository.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
public class WorkloadBaselineService {

    private final WorkloadRequestService requestService;
    private final RequestRepository requestRepository;
    private final TenantDatabaseContext tenantDatabaseContext;
    private final WorkloadExecutionDispatcher executionDispatcher;

    public WorkloadBaselineService(
            WorkloadRequestService requestService,
            RequestRepository requestRepository,
            TenantDatabaseContext tenantDatabaseContext,
            WorkloadExecutionDispatcher executionDispatcher) {
        this.requestService = requestService;
        this.requestRepository = requestRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
        this.executionDispatcher = executionDispatcher;
    }

    @Transactional
    public Request submit(String tenantId, String sql) {
        Request request = requestService.submit(tenantId, sql);
        tenantDatabaseContext.setTenantId(tenantId);

        Instant queueTime = Instant.now();
        requestRepository.queueBaselineRequest(request.getId(), queueTime);
        request.setStatus(RequestStatus.QUEUED);
        request.setQueueEntryTime(queueTime);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "Transaction synchronization is not active");
        }

        String requestId = request.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        executionDispatcher.dispatch(requestId, tenantId);
                    }
                });

        return request;
    }
}