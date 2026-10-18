package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantDatabaseContext;
import com.workloadgovernance.model.Metric;
import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.RequestStatus;
import com.workloadgovernance.repository.MetricRepository;
import com.workloadgovernance.repository.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ExecutionResultRecorder {

    private final RequestRepository requestRepository;
    private final MetricRepository metricRepository;
    private final TenantDatabaseContext tenantDatabaseContext;

    public ExecutionResultRecorder(
            RequestRepository requestRepository,
            MetricRepository metricRepository,
            TenantDatabaseContext tenantDatabaseContext) {
        this.requestRepository = requestRepository;
        this.metricRepository = metricRepository;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional
    public void record(
            Request request,
            RequestStatus finalStatus,
            Instant completionTime,
            long executionTimeMs,
            long responseTimeMs,
            long waitTimeMs,
            long rowsProcessed) {

        tenantDatabaseContext.setTenantId(
                request.getTenant().getId()
        );

        requestRepository.markExecutionFinished(
                request.getId(),
                finalStatus,
                completionTime
        );

        metricRepository.save(new Metric(
                request.getId(),
                executionTimeMs,
                responseTimeMs,
                waitTimeMs,
                rowsProcessed,
                completionTime
        ));
    }
}
