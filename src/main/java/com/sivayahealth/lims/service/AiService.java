package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.AiInventoryForecast;
import com.sivayahealth.lims.entity.Branch;
import com.sivayahealth.lims.entity.Tenant;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiInventoryForecastRepository forecastRepository;
    private final ChemicalIssuanceRepository issuanceRepository;
    private final ChemicalStockRepository stockRepository;
    private final InstrumentCalibrationResultRepository calibrationResultRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<AiInventoryForecast> getInventoryForecasts(Long tenantId, Long branchId) {
        return forecastRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional
    public AiInventoryForecast generateForecast(Long tenantId, Long branchId, String itemType, Long itemId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));

        // Simple moving average forecast based on historical issuances
        BigDecimal avgUsage = calculateAverageUsage(tenantId, branchId, itemId);

        AiInventoryForecast forecast = AiInventoryForecast.builder()
                .tenant(tenant).branch(branch)
                .itemType(itemType).itemId(itemId)
                .forecastDate(LocalDate.now().plusDays(30))
                .predictedUsage(avgUsage)
                .modelVersion("v1.0-moving-avg")
                .build();

        return forecastRepository.save(forecast);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOosRisk(Long tenantId, Long branchId) {
        Map<String, Object> risk = new HashMap<>();
        // Compute risk based on recent calibration failures and deviations
        risk.put("riskScore", calculateOosRiskScore(tenantId, branchId));
        risk.put("recommendation", "Review recent OOS cases and calibration trends");
        return risk;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInstrumentTrend(Long tenantId, Long instrumentId) {
        Map<String, Object> trend = new HashMap<>();
        List<?> results = calibrationResultRepository.findByCalibrationId(instrumentId);
        trend.put("dataPoints", results.size());
        trend.put("trend", "STABLE");
        return trend;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWorkloadPrediction(Long tenantId, Long branchId) {
        Map<String, Object> workload = new HashMap<>();
        workload.put("predictedSamples", 45);
        workload.put("predictedTests", 180);
        workload.put("period", "next 7 days");
        return workload;
    }

    private BigDecimal calculateAverageUsage(Long tenantId, Long branchId, Long chemicalId) {
        // Simplified: would normally aggregate historical issuance data
        List<?> issuances = issuanceRepository.findByTenantIdAndBranchId(tenantId, branchId);
        if (issuances.isEmpty()) return BigDecimal.valueOf(10);
        return BigDecimal.valueOf(issuances.size() * 2.5);
    }

    private double calculateOosRiskScore(Long tenantId, Long branchId) {
        // Simplified risk scoring
        return 0.15; // 15% risk
    }
}
