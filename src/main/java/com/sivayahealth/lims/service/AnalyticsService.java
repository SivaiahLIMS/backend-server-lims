package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final DocumentTestResultRepository documentTestResultRepository;
    private final InstrumentReadingRepository instrumentReadingRepository;
    private final InstrumentReservationRepository instrumentReservationRepository;
    private final PredictiveAlertRepository predictiveAlertRepository;
    private final InstrumentMetricSnapshotRepository instrumentMetricSnapshotRepository;
    private final AppUserRepository appUserRepository;
    private final TaskMasterRepository taskMasterRepository;

    public Map<String, Object> getOosTrend(Long tenantId, Long branchId, LocalDate from, LocalDate to) {
        List<DocumentTestResult> oosResults = documentTestResultRepository.findByTenantIdAndBranchIdAndOosTrue(tenantId, branchId);

        Map<LocalDate, Long> byDate = oosResults.stream()
                .filter(r -> {
                    LocalDate d = r.getCreatedAt().toLocalDate();
                    return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
                })
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<Map<String, Object>> points = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", e.getKey().toString());
                    point.put("oosCount", e.getValue());
                    return point;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("points", points);
        result.put("totalOos", oosResults.size());
        return result;
    }

    public Map<String, Object> getInstrumentUtilization(Long instrumentId, LocalDate from, LocalDate to) {
        List<InstrumentReservation> reservations = instrumentReservationRepository.findByInstrument_Id(instrumentId);
        List<InstrumentReading> readings = instrumentReadingRepository.findByInstrument_Id(instrumentId);

        Map<String, Object> result = new HashMap<>();
        result.put("instrumentId", instrumentId);
        result.put("totalReservations", reservations.size());
        result.put("approvedReservations", reservations.stream().filter(r -> "APPROVED".equals(r.getStatus())).count());
        result.put("totalReadings", readings.size());

        List<InstrumentMetricSnapshot> snapshots = instrumentMetricSnapshotRepository
                .findByInstrument_IdOrderByMetricDateAsc(instrumentId);
        result.put("metricSnapshots", snapshots);

        return result;
    }

    public List<PredictiveAlert> getPredictiveAlerts(Long tenantId, Long branchId) {
        return predictiveAlertRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    public List<PredictiveAlert> getOpenPredictiveAlerts(Long tenantId, Long branchId) {
        return predictiveAlertRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, "OPEN");
    }

    @Transactional
    public PredictiveAlert acknowledgePredictiveAlert(Long id, Long userId) {
        PredictiveAlert alert = predictiveAlertRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Predictive alert not found: " + id));
        AppUser user = appUserRepository.findById(userId).orElse(null);
        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(user);
        alert.setAcknowledgedAt(java.time.LocalDateTime.now());
        return predictiveAlertRepository.save(alert);
    }

    public Map<String, Object> getTaskMetrics(Long tenantId, Long branchId) {
        List<TaskMaster> tasks = taskMasterRepository.findByTenantIdAndBranchId(tenantId, branchId);
        Map<String, Long> byStatus = tasks.stream()
                .collect(Collectors.groupingBy(TaskMaster::getStatus, Collectors.counting()));
        Map<String, Long> byType = tasks.stream()
                .collect(Collectors.groupingBy(TaskMaster::getType, Collectors.counting()));

        Map<String, Object> result = new HashMap<>();
        result.put("byStatus", byStatus);
        result.put("byType", byType);
        result.put("total", tasks.size());
        return result;
    }

    @Transactional
    public InstrumentMetricSnapshot saveMetricSnapshot(InstrumentMetricSnapshot snapshot) {
        return instrumentMetricSnapshotRepository.save(snapshot);
    }

    @Transactional
    public PredictiveAlert createPredictiveAlert(PredictiveAlert alert) {
        return predictiveAlertRepository.save(alert);
    }
}
