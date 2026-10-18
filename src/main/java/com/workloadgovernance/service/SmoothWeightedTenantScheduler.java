package com.workloadgovernance.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SmoothWeightedTenantScheduler {
    private final Map<String, Long> currentWeights = new HashMap<>();

    public synchronized void retainTenants(
            List<TenantSchedulingService.TenantShare> tenants) {
        Set<String> activeIds = tenants.stream()
                .map(TenantSchedulingService.TenantShare::tenantId)
                .collect(Collectors.toSet());
        currentWeights.keySet().retainAll(activeIds);
    }

    public synchronized TenantSchedulingService.TenantShare next(
            List<TenantSchedulingService.TenantShare> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("At least one tenant is required");
        }

        long totalWeight = 0;
        TenantSchedulingService.TenantShare selected = null;
        long highestCurrentWeight = Long.MIN_VALUE;

        for (TenantSchedulingService.TenantShare tenant : candidates) {
            if (tenant.weight() <= 0) {
                throw new IllegalArgumentException("Tenant weight must be positive");
            }

            totalWeight += tenant.weight();
            long current = currentWeights.getOrDefault(tenant.tenantId(), 0L)
                    + tenant.weight();
            currentWeights.put(tenant.tenantId(), current);

            if (selected == null || current > highestCurrentWeight) {
                selected = tenant;
                highestCurrentWeight = current;
            }
        }

        String selectedId = selected.tenantId();
        currentWeights.put(
                selectedId,
                currentWeights.get(selectedId) - totalWeight);
        return selected;
    }
}
