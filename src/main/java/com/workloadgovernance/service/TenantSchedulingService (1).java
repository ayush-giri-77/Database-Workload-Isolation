package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantDatabaseContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TenantSchedulingService {
    private final JdbcTemplate jdbcTemplate;
    private final TenantDatabaseContext tenantDatabaseContext;

    public TenantSchedulingService(
            JdbcTemplate jdbcTemplate,
            TenantDatabaseContext tenantDatabaseContext) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantDatabaseContext = tenantDatabaseContext;
    }

    @Transactional(readOnly = true)
    public List<TenantShare> findTenantShares() {
        List<String> tenantIds = jdbcTemplate.queryForList(
                "SELECT tenant_id FROM public.list_tenant_ids_for_scheduler() ORDER BY tenant_id",
                String.class);

        List<TenantShare> shares = new ArrayList<>();
        for (String tenantId : tenantIds) {
            tenantDatabaseContext.setTenantId(tenantId);
            List<Integer> weights = jdbcTemplate.queryForList(
                    """
                    SELECT weight
                    FROM public.tenant
                    WHERE id = ? 
                    """,
                    Integer.class,
                    tenantId);

            if (!weights.isEmpty() && weights.get(0) != null && weights.get(0) > 0) {
                shares.add(new TenantShare(tenantId, weights.get(0)));
            }
        }
        return List.copyOf(shares);
    }

    public record TenantShare(String tenantId, int weight) {}
}
