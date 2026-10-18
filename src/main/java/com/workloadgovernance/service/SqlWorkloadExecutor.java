package com.workloadgovernance.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class SqlWorkloadExecutor {

    private final JdbcTemplate jdbcTemplate;

    public SqlWorkloadExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(
            readOnly = true,
            propagation = Propagation.REQUIRES_NEW
    )
    public long executeSelect(String tenantId, String sql) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(
                    "Tenant ID must not be blank"
            );
        }

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL must not be blank"
            );
        }

        // Validate SQL before making any database calls.
        String normalized = sql.stripLeading().toLowerCase(Locale.ROOT);

        if (!(normalized.startsWith("select")
                || normalized.startsWith("with"))
                || sql.contains(";")) {
            throw new IllegalArgumentException(
                    "Only a single SELECT or WITH query is allowed"
            );
        }

        // Set tenant context only after SQL validation succeeds.
        jdbcTemplate.queryForObject(
                "SELECT set_config('app.tenant_id', ?, true)",
                String.class,
                tenantId
        );

        // Execute SQL and count returned rows.
        Long rowCount = jdbcTemplate.query(
                connection -> {
                    var statement = connection.prepareStatement(sql);
                    statement.setQueryTimeout(10);
                    return statement;
                },
                resultSet -> {
                    long rows = 0;
                    while (resultSet.next()) {
                        rows++;
                    }
                    return rows;
                }
        );

        return rowCount != null ? rowCount : 0L;
    }
}

