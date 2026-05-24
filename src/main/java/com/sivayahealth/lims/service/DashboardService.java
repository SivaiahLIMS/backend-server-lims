package com.sivayahealth.lims.service;

import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CriticalAlertRepository alertRepository;
    private final InstrumentCalibrationScheduleRepository calibrationScheduleRepository;
    private final ChemicalRegistrationRepository chemicalRegistrationRepository;
    private final SampleRepository sampleRepository;
    private final OosCaseRepository oosCaseRepository;
    private final DeviationRepository deviationRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardData(Long tenantId, Long branchId) {
        Map<String, Object> data = new HashMap<>();

        data.put("criticalAlerts", alertRepository
                .findByTenantIdAndBranchIdAndResolvedAtIsNull(tenantId, branchId).size());

        data.put("calibrationDue", calibrationScheduleRepository
                .findByStatus("DUE").size());

        data.put("calibrationOverdue", calibrationScheduleRepository
                .findByStatus("OVERDUE").size());

        data.put("expiringChemicals", chemicalRegistrationRepository
                .findExpiringChemicals(tenantId, LocalDate.now().plusDays(30)).size());

        data.put("openOos", oosCaseRepository
                .findByTenantIdAndStatus(tenantId, "OPEN").size());

        data.put("openDeviations", deviationRepository
                .findByTenantIdAndStatus(tenantId, "OPEN").size());

        data.put("inProgressSamples", sampleRepository
                .findByTenantIdAndStatus(tenantId, "IN_PROGRESS").size());

        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWidgets(Long tenantId, Long branchId) {
        return getDashboardData(tenantId, branchId);
    }
}
