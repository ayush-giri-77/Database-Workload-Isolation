package com.workloadgovernance.repository;

import com.workloadgovernance.model.Metric;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class MetricRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Metric> metricRowMapper = (rs, rowNum) ->
            new Metric(
                    rs.getString("request_id"),
                    rs.getLong("execution_time_ms"),
                    rs.getLong("response_time_ms"),
                    rs.getLong("wait_time_ms"),
                    rs.getLong("rows_processed"),
                    rs.getTimestamp("recorded_at").toInstant()
            );

    public MetricRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Metric metric) {
        if (metric == null
                || metric.getRequestId() == null
                || metric.getRequestId().isBlank()
                || metric.getRecordedAt() == null) {
            throw new IllegalArgumentException(
                    "Metric, request ID, and recorded time are required"
            );
        }

        if (metric.getExecutionTimeMs() < 0
                || metric.getResponseTimeMs() < 0
                || metric.getWaitTimeMs() < 0
                || metric.getRowsProcessed() < 0) {
            throw new IllegalArgumentException(
                    "Metric durations and rows processed must not be negative"
            );
        }

        String sql = """
                INSERT INTO public.metric
                    (request_id, execution_time_ms, response_time_ms,
                     wait_time_ms, rows_processed, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (request_id) DO UPDATE SET
                    execution_time_ms = EXCLUDED.execution_time_ms,
                    response_time_ms = EXCLUDED.response_time_ms,
                    wait_time_ms = EXCLUDED.wait_time_ms,
                    rows_processed = EXCLUDED.rows_processed,
                    recorded_at = EXCLUDED.recorded_at
                """;

        jdbcTemplate.update(
                sql,
                metric.getRequestId(),
                metric.getExecutionTimeMs(),
                metric.getResponseTimeMs(),
                metric.getWaitTimeMs(),
                metric.getRowsProcessed(),
                Timestamp.from(metric.getRecordedAt())
        );
    }

    public List<Metric> findByRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("Request ID must not be blank");
        }

        String sql = """
                SELECT request_id, execution_time_ms, response_time_ms,
                       wait_time_ms, rows_processed, recorded_at
                FROM public.metric
                WHERE request_id = ?
                ORDER BY recorded_at
                """;

        return jdbcTemplate.query(sql, metricRowMapper, requestId);
    }
}