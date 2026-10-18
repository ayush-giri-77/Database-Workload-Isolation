package com.workloadgovernance.model;

public class WorkloadPolicy {

    private final String tenantId;
    private final WorkloadType workloadType;
    private final int maxConcurrentRequests;
    private final int maxQueueSize;
    private final boolean enabled;

    public WorkloadPolicy(
            String tenantId,
            WorkloadType workloadType,
            int maxConcurrentRequests,
            int maxQueueSize,
            boolean enabled) {
        this.tenantId = tenantId;
        this.workloadType = workloadType;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.maxQueueSize = maxQueueSize;
        this.enabled = enabled;
    }

    public String getTenantId() {
        return tenantId;
    }

    public WorkloadType getWorkloadType() {
        return workloadType;
    }

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
