# Database schema design

This document describes the initial PostgreSQL schema for Phase 3. It matches the current Tenant and Request models and introduces the persistence structures needed for per-workload policy and request metrics.

## Scope

The database stores tenant configuration, workload limits, submitted requests, and per-request measurements. Governance still happens in the Java middleware. The schema does not allocate physical CPU, memory, or disk resources.

The executable DDL is in [src/main/resources/db/schema.sql](../../src/main/resources/db/schema.sql). It is a manual schema script for this milestone; Spring Boot is not configured to run it automatically yet.

## Tables and Java mapping

| Table | Purpose | Current or planned Java mapping |
|---|---|---|
| tenant | Tenant identity and tenant-wide scheduling limits | Tenant.id, name, weight, maxConcurrency, active |
| workload_policy | Tenant-specific limits for each workload class | New persistence model, to be designed when repository work begins |
| workload_request | Submitted SQL and request lifecycle | Request.id, tenant.id, sql, workloadType, status, and lifecycle timestamps |
| metric | One measurement record per request | Planned metrics model; keyed by request_id |

IDs use VARCHAR(64) because the current Java models expose IDs as String. This avoids silently imposing a UUID format before ID generation is selected. Instant values map to PostgreSQL TIMESTAMPTZ.

## Relationships

- A tenant can have many requests.
- A tenant can have one policy for each workload type. (tenant_id, workload_type) is the composite primary key.
- A request has at most one metric row. metric.request_id is both its primary key and a foreign key to the request.

Deleting a tenant is restricted while it has requests, preserving request history. Its policy rows are removed with the tenant. A metric row is removed if its request is explicitly deleted.

## Constraints and indexes

- Tenant weight and tenant concurrency must be positive.
- Workload concurrency must be positive; queue capacity may be zero to disable waiting for that workload.
- Workload types and request statuses are checked against the current enum values.
- Metric durations and row counts cannot be negative. They are nullable because measurements are not known when a request is first submitted.
- Indexes support common lookups by tenant and arrival time, status and arrival time, and workload type and status.

## Metric definitions

The application should write metrics after a request reaches a terminal state:

- execution_time_ms = completion time minus execution start time.
- response_time_ms = completion time minus arrival time.
- wait_time_ms = execution start time minus queue entry time for queued requests; use zero for requests that execute immediately without entering a queue.
- rows_processed is populated only when the executor can report a meaningful count.

These are elapsed durations, stored as integer milliseconds. They should be derived from lifecycle timestamps so the metric calculation remains explainable and can be recomputed.

## Current limitations and next step

WORKLOAD_POLICY and metric are schema-first: their Java models and repositories do not exist yet. The script is not wired to Spring Boot, and no PostgreSQL schema execution has been verified from this workspace. Phase 4 should add PostgreSQL connectivity and repositories, then apply this DDL to a development database and verify the tables, constraints, and indexes.

The schema supports the current V1 foundation; it does not yet model system-level policy, workload priority, SLO targets, ML predictions, or adaptive feedback.
