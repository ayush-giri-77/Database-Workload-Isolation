-- Initial Phase 3 schema for Adaptive Multi-Tenant Workload Governance.
-- Apply once to an empty project database. This file is not auto-run by Spring Boot yet.

CREATE TABLE tenant (
    id                  VARCHAR(64) PRIMARY KEY,
    name                VARCHAR(100) NOT NULL UNIQUE,
    weight              INTEGER NOT NULL CHECK (weight > 0),
    max_concurrency     INTEGER NOT NULL CHECK (max_concurrency > 0),
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE workload_policy (
    tenant_id                   VARCHAR(64) NOT NULL,
    workload_type               VARCHAR(16) NOT NULL,
    enabled                     BOOLEAN NOT NULL DEFAULT TRUE,
    max_concurrent_requests     INTEGER NOT NULL CHECK (max_concurrent_requests > 0),
    max_queue_size              INTEGER NOT NULL CHECK (max_queue_size >= 0),
    CONSTRAINT pk_workload_policy PRIMARY KEY (tenant_id, workload_type),
    CONSTRAINT fk_workload_policy_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE CASCADE,
    CONSTRAINT ck_workload_policy_type
        CHECK (workload_type IN ('LIGHT', 'MEDIUM', 'HEAVY'))
);

CREATE TABLE workload_request (
    id                      VARCHAR(64) PRIMARY KEY,
    tenant_id               VARCHAR(64) NOT NULL,
    sql_text                TEXT NOT NULL,
    workload_type           VARCHAR(16),
    status                  VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED',
    arrival_time            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    queue_entry_time        TIMESTAMPTZ,
    execution_start_time    TIMESTAMPTZ,
    completion_time         TIMESTAMPTZ,
    CONSTRAINT fk_workload_request_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenant (id) ON DELETE RESTRICT,
    CONSTRAINT ck_workload_request_type
        CHECK (workload_type IS NULL OR workload_type IN ('LIGHT', 'MEDIUM', 'HEAVY')),
    CONSTRAINT ck_workload_request_status
        CHECK (status IN ('SUBMITTED', 'CLASSIFIED', 'QUEUED', 'RUNNING', 'COMPLETED', 'REJECTED', 'FAILED'))
);

CREATE TABLE metric (
    request_id          VARCHAR(64) PRIMARY KEY,
    wait_time_ms        BIGINT CHECK (wait_time_ms >= 0),
    execution_time_ms   BIGINT CHECK (execution_time_ms >= 0),
    response_time_ms    BIGINT CHECK (response_time_ms >= 0),
    rows_processed      BIGINT CHECK (rows_processed >= 0),
    recorded_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_metric_request
        FOREIGN KEY (request_id) REFERENCES workload_request (id) ON DELETE CASCADE
);

CREATE INDEX idx_workload_request_tenant_arrival
    ON workload_request (tenant_id, arrival_time);

CREATE INDEX idx_workload_request_status_arrival
    ON workload_request (status, arrival_time);

CREATE INDEX idx_workload_request_workload_status
    ON workload_request (workload_type, status);
