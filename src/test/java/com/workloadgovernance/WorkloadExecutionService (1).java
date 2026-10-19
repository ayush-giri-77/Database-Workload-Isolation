package com.workloadgovernance.service;

import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class WorkloadExecutionService {

    private static final Logger log =
            LoggerFactory.getLogger(WorkloadExecutionService.class);

    private final TenantScopedRequestReader tenantScopedRequestReader;
    private final SqlWorkloadExecutor sqlWorkloadExecutor;
    private final ExecutionResultRecorder executionResultRecorder;
    private final ExecutionStartRecorder executionStartRecorder;

    public WorkloadExecutionService(
            TenantScopedRequestReader tenantScopedRequestReader,
            SqlWorkloadExecutor sqlWorkloadExecutor,
            ExecutionResultRecorder executionResultRecorder,
            ExecutionStartRecorder executionStartRecorder) {
        this.tenantScopedRequestReader = tenantScopedRequestReader;
        this.sqlWorkloadExecutor = sqlWorkloadExecutor;
        this.executionResultRecorder = executionResultRecorder;
        this.executionStartRecorder = executionStartRecorder;
    }

    public RequestStatus execute(String requestId, String tenantId) {
        Request request =
                tenantScopedRequestReader.findById(tenantId, requestId);

        if (request.getStatus() == RequestStatus.QUEUED) {
            Instant executionStartTime =
                    executionStartRecorder.startBaselineRequest(
                            requestId, tenantId);
            request.setStatus(RequestStatus.RUNNING);
            request.setExecutionStartTime(executionStartTime);
        }

        if (request.getStatus() != RequestStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only RUNNING or baseline QUEUED requests can be executed");
        }

        long startNanos = System.nanoTime();
        long rowsProcessed = 0;
        RequestStatus finalStatus;

        try {
            rowsProcessed = sqlWorkloadExecutor.executeSelect(
                    tenantId,
                    request.getSql());
            finalStatus = RequestStatus.COMPLETED;
        } catch (RuntimeException exception) {
            log.error(
                    "Workload execution failed for request {}",
                    requestId,
                    exception);
            finalStatus = RequestStatus.FAILED;
        }

        long executionTimeMs =
                Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        Instant completionTime = Instant.now();
        long responseTimeMs = elapsedMillis(
                request.getArrivalTime(), completionTime);
        long waitTimeMs = elapsedMillis(
                request.getArrivalTime(), request.getExecutionStartTime());

        executionResultRecorder.record(
                request,
                finalStatus,
                completionTime,
                executionTimeMs,
                responseTimeMs,
                waitTimeMs,
                rowsProcessed);

        return finalStatus;
    }

    private static long elapsedMillis(Instant start, Instant end) {
        if (start == null || end == null) {
            return 0;
        }
        return Math.max(0, Duration.between(start, end).toMillis());
    }
}