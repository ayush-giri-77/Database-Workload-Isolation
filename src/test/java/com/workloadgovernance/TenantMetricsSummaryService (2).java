package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantDatabaseContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Service
public class TenantMetricsSummaryService {

    private static final RowMapper<MetricsSummary> SUMMARY_ROW_MAPPER =
            (rs, rowNum) -> new MetricsSummary(
                    rs.getString("tenant_id"),
                    rs.getLong("total_requests"),
                    rs.getLong("submitted"),
                    rs.getLong("classified"),
                    rs.getLong("queued"),
                    rs.getLong("running"),
                    rs.getLong("completed"),
                    rs.getLong("rejected"),
                    rs.getLong("failed"),
                    rs.getLong("requests_with_metrics"),
                    rs.getDouble("average_wait_time_ms"),
                    rs.getDouble("average_execution_time_ms"),
                    rs.getDouble("average_response_time_ms"),
                    rs.getLong("total_rows_processed"));

    private final JdbcTemplate jdbcTemplate;
    private final TenantDatabaseContext tenantDatabaseContext;

    public TenantMetricsSummaryService(
            JdbcTemplate jdbcTemplate,
            TenantDatabaseContext tenantDatabaseContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional(readOnly = true)
    public MetricsSummary summarize(String tenantId, Instant since) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant ID must not be blank");
        }

        tenantDatabaseContext.setTenantId(tenantId);

        String timeFilter = since == null
                ? ""
                : " AND wr.arrival_time >= ?";

        String sql = """
                SELECT ? AS tenant_id,
                       COUNT(*) AS total_requests,
                       COUNT(*) FILTER (
                           WHERE wr.status = 'SUBMITTED'
                       ) AS submitted,
                       COUNT(*) FILTER (
                           WHERE wr.status = 'CLASSIFIED'
                       ) AS classified,
                       COUNT(*) FILTER (
                           WHERE wr.status = 'QUEUED'
                       ) AS queued,
                       COUNT(*) FILTER (
                           WHERE wr.status = 'RUNNING'
                       ) AS running,
                       COUNT(*) FILTER (
                           WHERE wr.status = 'COMPLETED'
                       ) AS completed,
                       COUNT(*) FILTER (
                           WHERE wr.status = 'REJECTED'
                       ) AS rejected,
                       COUNT(*) FILTER (
                           WHERE wr.status = 'FAILED'
                       ) AS failed,
                       COUNT(m.request_id) AS requests_with_metrics,
                       COALESCE(
                           AVG(m.wait_time_ms), 0
                       ) AS average_wait_time_ms,
                       COALESCE(
                           AVG(m.execution_time_ms), 0
                       ) AS average_execution_time_ms,
                       COALESCE(
                           AVG(m.response_time_ms), 0
                       ) AS average_response_time_ms,
                       COALESCE(
                           SUM(m.rows_processed), 0
                       ) AS total_rows_processed
                FROM public.workload_request wr
                LEFT JOIN public.metric m
                    ON m.request_id = wr.id
                WHERE wr.tenant_id = ?
                """ + timeFilter;

        Object[] parameters = since == null
                ? new Object[]{tenantId, tenantId}
                : new Object[]{
                tenantId,
                tenantId,
                Timestamp.from(since)
        };

        return jdbcTemplate.queryForObject(
                sql,
                SUMMARY_ROW_MAPPER,
                parameters);
    }

    public record MetricsSummary(
            String tenantId,
            long totalRequests,
            long submitted,
            long classified,
            long queued,
            long running,
            long completed,
            long rejected,
            long failed,
            long requestsWithMetrics,
            double averageWaitTimeMs,
            double averageExecutionTimeMs,
            double averageResponseTimeMs,
            long totalRowsProcessed) {}
}
