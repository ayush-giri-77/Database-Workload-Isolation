package com.workloadgovernance.service;

import com.workloadgovernance.model.RequestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class WorkloadExecutionDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(WorkloadExecutionDispatcher.class);

    private final WorkloadExecutionService executionService;
    private final ThreadPoolTaskExecutor taskExecutor;

    public WorkloadExecutionDispatcher(
            WorkloadExecutionService executionService,
            @Qualifier("workloadTaskExecutor")
            ThreadPoolTaskExecutor taskExecutor) {
        this.executionService = executionService;
        this.taskExecutor = taskExecutor;
    }

    public boolean hasCapacity() {
        return taskExecutor.getActiveCount() < taskExecutor.getMaxPoolSize()
                && taskExecutor.getThreadPoolExecutor().getQueue().isEmpty();
    }

    @Async("workloadTaskExecutor")
    public void dispatch(String requestId, String tenantId) {
        log.info(
                "Worker starting request {}, tenant {}",
                requestId,
                tenantId
        );

        try {
            RequestStatus status =
                    executionService.execute(requestId, tenantId);

            log.info(
                    "Worker finished request {}, tenant {}, status {}",
                    requestId,
                    tenantId,
                    status
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Worker failed for request {}, tenant {}",
                    requestId,
                    tenantId,
                    exception
            );
        }
    }
}