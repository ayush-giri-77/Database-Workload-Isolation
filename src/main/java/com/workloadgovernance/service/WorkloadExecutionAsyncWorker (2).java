package com.workloadgovernance.service;

import com.workloadgovernance.event.WorkloadExecutionRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class WorkloadExecutionAsyncWorker {

    private static final Logger log =
            LoggerFactory.getLogger(WorkloadExecutionAsyncWorker.class);

    private final WorkloadExecutionService executionService;

    public WorkloadExecutionAsyncWorker(
            WorkloadExecutionService executionService) {
        this.executionService = executionService;
    }

    @Async("workloadTaskExecutor")
    public void execute(WorkloadExecutionRequested event) {
        log.info(
                "ASYNC WORKER STARTED: requestId={}, tenantId={}",
                event.requestId(),
                event.tenantId());

        try {
            var status = executionService.execute(
                    event.requestId(),
                    event.tenantId());

            log.info(
                    "ASYNC WORKER FINISHED: requestId={}, status={}",
                    event.requestId(),
                    status);
        } catch (RuntimeException exception) {
            log.error(
                    "ASYNC WORKER FAILED: requestId={}",
                    event.requestId(),
                    exception);
        }
    }
}

