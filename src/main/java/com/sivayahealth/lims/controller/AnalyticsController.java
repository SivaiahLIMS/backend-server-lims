package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Trend analytics, utilization, and predictive intelligence")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/products/{id}/oos-trend")
    @Operation(summary = "Get OOS trend data for a product")
    public Map<String, Object> getOosTrend(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value =  "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.getOosTrend(tenantId, branchId, from, to);
    }

    @GetMapping("/instruments/{id}/utilization")
    @Operation(summary = "Get instrument utilization analytics")
    public Map<String, Object> getInstrumentUtilization(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.getInstrumentUtilization(id, from, to);
    }

    @GetMapping("/predictive-alerts")
    @Operation(summary = "Get predictive alerts")
    public List<PredictiveAlert> getPredictiveAlerts(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false, defaultValue = "false") boolean openOnly) {
        return openOnly
                ? analyticsService.getOpenPredictiveAlerts(tenantId, branchId)
                : analyticsService.getPredictiveAlerts(tenantId, branchId);
    }

    @PostMapping("/predictive-alerts/{id}/acknowledge")
    @Operation(summary = "Acknowledge a predictive alert")
    public PredictiveAlert acknowledgeAlert(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        return analyticsService.acknowledgePredictiveAlert(id, userId);
    }

    @GetMapping("/tasks/metrics")
    @Operation(summary = "Get task metrics overview")
    public Map<String, Object> getTaskMetrics(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        return analyticsService.getTaskMetrics(tenantId, branchId);
    }
}
