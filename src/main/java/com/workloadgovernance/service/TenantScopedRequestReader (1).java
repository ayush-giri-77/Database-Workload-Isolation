package com.workloadgovernance.service;

import com.workloadgovernance.database.TenantDatabaseContext;
import com.workloadgovernance.model.Metric;
import com.workloadgovernance.model.Request;
import com.workloadgovernance.repository.MetricRepository;
import com.workloadgovernance.repository.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TenantScopedRequestReader {

    private final TenantDatabaseContext tenantDatabaseContext;
    private final RequestRepository requestRepository;
    private final MetricRepository metricRepository;

    public TenantScopedRequestReader(
            TenantDatabaseContext tenantDatabaseContext,
            RequestRepository requestRepository,
            MetricRepository metricRepository) {
        this.tenantDatabaseContext = tenantDatabaseContext;
        this.requestRepository = requestRepository;
        this.metricRepository = metricRepository;
    }

    @Transactional(readOnly = true)
    public Request findById(String tenantId, String requestId) {
        tenantDatabaseContext.setTenantId(tenantId);

        return requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Request not found: " + requestId));
    }

    @Transactional(readOnly = true)
    public Optional<Request> findOwnedById(
            String tenantId,
            String requestId) {

        tenantDatabaseContext.setTenantId(tenantId);

        return requestRepository.findById(requestId)
                .filter(request ->
                        tenantId.equals(request.getTenant().getId()));
    }

    @Transactional(readOnly = true)
    public Optional<List<Metric>> findMetricsByRequestId(
            String tenantId,
            String requestId) {

        tenantDatabaseContext.setTenantId(tenantId);

        Optional<Request> request = requestRepository.findById(requestId)
                .filter(found ->
                        tenantId.equals(found.getTenant().getId()));

        if (request.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                metricRepository.findByRequestId(requestId));
    }
}
