package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytics", description = "Trend analytics, utilization, and predictive intelligence")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/products/{id}/oos-trend")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get OOS trend data for a product",
               description = "Requires: ANALYTICS_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getOosTrend(
            @PathVariable Long id,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getOosTrend(u.getTenantId(), branchId, from, to));
    }

    @GetMapping("/instruments/{id}/utilization")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get instrument utilization analytics",
               description = "Requires: ANALYTICS_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Instrument not found")
    })
    public ResponseEntity<Map<String, Object>> getInstrumentUtilization(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(analyticsService.getInstrumentUtilization(id, from, to));
    }

    @GetMapping("/predictive-alerts")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get predictive alerts for branch",
               description = "Requires: ANALYTICS_VIEW. Use openOnly=true to filter to unacknowledged alerts.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PredictiveAlert>> getPredictiveAlerts(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false, defaultValue = "false") boolean openOnly,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<PredictiveAlert> result = openOnly
                ? analyticsService.getOpenPredictiveAlerts(u.getTenantId(), branchId)
                : analyticsService.getPredictiveAlerts(u.getTenantId(), branchId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/predictive-alerts/{id}/acknowledge")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Acknowledge a predictive alert",
               description = "Requires: ANALYTICS_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Acknowledged"),
        @ApiResponse(responseCode = "404", description = "Alert not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<PredictiveAlert> acknowledgeAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.acknowledgePredictiveAlert(id, u.getUser().getId()));
    }

    @GetMapping("/tasks/metrics")
    @PreAuthorize("hasAuthority('ANALYTICS_VIEW')")
    @Operation(summary = "Get task metrics overview for branch",
               description = "Requires: ANALYTICS_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getTaskMetrics(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(analyticsService.getTaskMetrics(u.getTenantId(), branchId));
    }
}
