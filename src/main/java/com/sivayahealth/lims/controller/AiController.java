package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.AiInventoryForecast;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI Module", description = "AI-driven forecasting, OOS risk, trend analysis")
public class AiController {

    private final AiService aiService;

    @GetMapping("/inventory-forecast")
    @PreAuthorize("hasAuthority('AI_INVENTORY_FORECAST_VIEW')")
    @Operation(summary = "Get AI inventory forecasts")
    public ResponseEntity<List<AiInventoryForecast>> getInventoryForecast(
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(aiService.getInventoryForecasts(u.getTenantId(), branchId));
    }

    @PostMapping("/orders/auto-initiate")
    @PreAuthorize("hasAuthority('AI_AUTO_ORDER_INITIATE')")
    @Operation(summary = "AI auto-initiate order forecast")
    public ResponseEntity<AiInventoryForecast> autoInitiateForecast(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                aiService.generateForecast(
                        u.getTenantId(),
                        ((Number) body.get("branchId")).longValue(),
                        (String) body.get("itemType"),
                        ((Number) body.get("itemId")).longValue()
                )
        );
    }

    @GetMapping("/oos-risk")
    @PreAuthorize("hasAuthority('AI_OOS_RISK_VIEW')")
    @Operation(summary = "Get OOS risk assessment")
    public ResponseEntity<Map<String, Object>> getOosRisk(
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(aiService.getOosRisk(u.getTenantId(), branchId));
    }

    @GetMapping("/instrument-trend")
    @PreAuthorize("hasAuthority('AI_INSTRUMENT_TREND_VIEW')")
    @Operation(summary = "Get instrument calibration trend")
    public ResponseEntity<Map<String, Object>> getInstrumentTrend(
            @RequestParam Long instrumentId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(aiService.getInstrumentTrend(u.getTenantId(), instrumentId));
    }

    @GetMapping("/workload")
    @PreAuthorize("hasAuthority('AI_WORKLOAD_VIEW')")
    @Operation(summary = "Get workload prediction")
    public ResponseEntity<Map<String, Object>> getWorkload(
            @RequestParam Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(aiService.getWorkloadPrediction(u.getTenantId(), branchId));
    }
}
