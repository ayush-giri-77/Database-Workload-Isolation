package com.workloadgovernance.model;

import java.time.Instant;

public class Metric {

    private final String requestId;
    private final long executionTimeMs;
    private final long responseTimeMs;
    private final long waitTimeMs;
    private final long rowsProcessed;
    private final Instant recordedAt;

    public Metric(
            String requestId,
            long executionTimeMs,
            long responseTimeMs,
            long waitTimeMs,
            long rowsProcessed,
            Instant recordedAt) {
        this.requestId = requestId;
        this.executionTimeMs = executionTimeMs;
        this.responseTimeMs = responseTimeMs;
        this.waitTimeMs = waitTimeMs;
        this.rowsProcessed = rowsProcessed;
        this.recordedAt = recordedAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public long getWaitTimeMs() {
        return waitTimeMs;
    }

    public long getRowsProcessed() {
        return rowsProcessed;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}