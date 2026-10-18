package com.workloadgovernance.service;

import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.WorkloadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@ConditionalOnProperty(
        prefix = "workload.governance",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class QueuePollingWorker {
    private static final Logger log =
            LoggerFactory.getLogger(QueuePollingWorker.class);
    private static final Duration STALE_AFTER = Duration.ofMinutes(1);

    private final TenantSchedulingService tenantSchedulingService;
    private final SmoothWeightedTenantScheduler weightedScheduler;
    private final QueuePromotionService queuePromotionService;
    private final WorkloadExecutionDispatcher executionDispatcher;

    public QueuePollingWorker(
            TenantSchedulingService tenantSchedulingService,
            SmoothWeightedTenantScheduler weightedScheduler,
            QueuePromotionService queuePromotionService,
            WorkloadExecutionDispatcher executionDispatcher) {
        this.tenantSchedulingService = tenantSchedulingService;
        this.weightedScheduler = weightedScheduler;
        this.queuePromotionService = queuePromotionService;
        this.executionDispatcher = executionDispatcher;
    }

    @Scheduled(fixedDelayString = "${workload.queue.poll-interval-ms:1000}")
    public void poll() {
        List<TenantSchedulingService.TenantShare> tenants;
        try {
            tenants = tenantSchedulingService.findTenantShares();
        } catch (RuntimeException exception) {
            log.error("Could not load active tenant scheduling weights", exception);
            return;
        }

        if (tenants.isEmpty()) {
            return;
        }
        weightedScheduler.retainTenants(tenants);

        for (TenantSchedulingService.TenantShare tenant : tenants) {
            for (WorkloadType type : WorkloadType.values()) {
                try {
                    int recovered = queuePromotionService.recoverStaleRunning(
                            tenant.tenantId(), type, STALE_AFTER);
                    if (recovered > 0) {
                        log.warn(
                                "Marked {} stale RUNNING request(s) FAILED "
                                        + "for tenant {} and type {}",
                                recovered, tenant.tenantId(), type);
                    }
                } catch (RuntimeException exception) {
                    log.error(
                            "Stale-request recovery failed for tenant {} and type {}",
                            tenant.tenantId(), type, exception);
                }
            }
        }

        WorkloadType[] types = WorkloadType.values();
        int nextTypeIndex = 0;
        Set<String> tenantsWithoutWork = new HashSet<>();

        while (executionDispatcher.hasCapacity()
                && tenantsWithoutWork.size() < tenants.size()) {
            List<TenantSchedulingService.TenantShare> candidates = tenants.stream()
                    .filter(tenant -> !tenantsWithoutWork.contains(tenant.tenantId()))
                    .toList();
            TenantSchedulingService.TenantShare tenant =
                    weightedScheduler.next(candidates);

            Request claimed = null;
            int claimedTypeIndex = -1;
            for (int offset = 0; offset < types.length; offset++) {
                int typeIndex = (nextTypeIndex + offset) % types.length;
                Optional<Request> request = queuePromotionService.claimNext(
                        tenant.tenantId(), types[typeIndex]);
                if (request.isPresent()) {
                    claimed = request.get();
                    claimedTypeIndex = typeIndex;
                    break;
                }
            }

            if (claimed == null) {
                tenantsWithoutWork.add(tenant.tenantId());
                continue;
            }

            executionDispatcher.dispatch(claimed.getId(), tenant.tenantId());
            log.info(
                    "Dispatched request {} for tenant {} (weight {})",
                    claimed.getId(), tenant.tenantId(), tenant.weight());
            nextTypeIndex = (claimedTypeIndex + 1) % types.length;
            tenantsWithoutWork.clear();
        }
    }
}
