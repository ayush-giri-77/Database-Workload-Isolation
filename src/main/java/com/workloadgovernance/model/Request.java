package com.workloadgovernance.model;
import java.time.Instant;

public class Request {
    private String id;
    private Tenant tenant;
    private String sql;
    private WorkloadType workloadType;
    private RequestStatus status;

    private Instant arrivalTime;
    private Instant queueEntryTime;
    private Instant executionStartTime;
    private Instant completionTime;

    public Request() {
        this.status = RequestStatus.SUBMITTED;
        this.arrivalTime = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public WorkloadType getWorkloadType() {
        return workloadType;
    }

    public void setWorkloadType(WorkloadType workloadType) {
        this.workloadType = workloadType;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Instant arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Instant getQueueEntryTime() {
        return queueEntryTime;
    }

    public void setQueueEntryTime(Instant queueEntryTime) {
        this.queueEntryTime = queueEntryTime;
    }

    public Instant getExecutionStartTime() {
        return executionStartTime;
    }

    public void setExecutionStartTime(Instant executionStartTime) {
        this.executionStartTime = executionStartTime;
    }

    public Instant getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(Instant completionTime) {
        this.completionTime = completionTime;
    }
}