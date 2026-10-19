package com.workloadgovernance.service;

import com.workloadgovernance.model.Request;
import com.workloadgovernance.model.RequestStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WorkloadSubmissionService {

    private final WorkloadRequestService requestService;
    private final WorkloadClassificationService classificationService;
    private final WorkloadAdmissionService admissionService;
    private final WorkloadBaselineService baselineService;
    private final boolean governanceEnabled;

    public WorkloadSubmissionService(
            WorkloadRequestService requestService,
            WorkloadClassificationService classificationService,
            WorkloadAdmissionService admissionService,
            WorkloadBaselineService baselineService,
            @Value("${workload.governance.enabled:true}") boolean governanceEnabled) {
        this.requestService = requestService;
        this.classificationService = classificationService;
        this.admissionService = admissionService;
        this.baselineService = baselineService;
        this.governanceEnabled = governanceEnabled;
    }

    public Request submit(String tenantId, String sql) {
        if (!governanceEnabled) {
            return baselineService.submit(tenantId, sql);
        }

        Request request = requestService.submit(tenantId, sql);

        request = classificationService.classify(
                tenantId,
                request.getId());

        if (request.getStatus() == RequestStatus.CLASSIFIED) {
            RequestStatus admissionStatus =
                    admissionService.admit(request.getId(), tenantId);
            request.setStatus(admissionStatus);
        }

        return request;
    }

    public boolean isGovernanceEnabled() {
        return governanceEnabled;
    }
}
