package com.workloadgovernance.repository;

import com.workloadgovernance.model.Tenant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TenantRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Tenant> tenantRowMapper = (resultSet, rowNumber) ->
            new Tenant(
                    resultSet.getString("id"),
                    resultSet.getString("name"),
                    resultSet.getInt("weight"),
                    resultSet.getInt("max_concurrency"),
                    resultSet.getBoolean("active")
            );

    public TenantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Tenant> findAll() {
        String sql = """
                SELECT id, name, weight, max_concurrency, active
                FROM public.tenant
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, tenantRowMapper);
    }

    public Optional<Tenant> findById(String id) {
        String sql = """
                SELECT id, name, weight, max_concurrency, active
                FROM public.tenant
                WHERE id = ?
                """;

        List<Tenant> tenants = jdbcTemplate.query(
                sql,
                tenantRowMapper,
                id
        );

        return tenants.stream().findFirst();
    }
}
