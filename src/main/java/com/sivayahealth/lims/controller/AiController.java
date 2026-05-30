package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.ai.AutoInitiateForecastRequest;
import com.sivayahealth.lims.entity.AiInventoryForecast;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Module", description = "AI-driven forecasting, OOS risk, trend analysis")
public class AiController {

    private final AiService aiService;

    @GetMapping("/inventory-forecast")
    @PreAuthorize("hasAuthority('AI_INVENTORY_FORECAST_VIEW')")
    @Operation(summary = "Get AI inventory forecasts for branch",
               description = "Requires: AI_INVENTORY_FORECAST_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<AiInventoryForecast>> getInventoryForecast(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(aiService.getInventoryForecasts(u.getTenantId(), branchId));
    }

    @PostMapping("/orders/auto-initiate")
    @PreAuthorize("hasAuthority('AI_AUTO_ORDER_INITIATE')")
    @Operation(summary = "AI auto-initiate order forecast",
               description = "Requires: AI_AUTO_ORDER_INITIATE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Forecast generated"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<AiInventoryForecast> autoInitiateForecast(
            @RequestBody AutoInitiateForecastRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                aiService.generateForecast(
                        u.getTenantId(),
                        body.getBranchId(),
                        body.getItemType(),
                        body.getItemId()
                )
        );
    }

    @GetMapping("/oos-risk")
    @PreAuthorize("hasAuthority('AI_OOS_RISK_VIEW')")
    @Operation(summary = "Get OOS risk assessment for branch",
               description = "Requires: AI_OOS_RISK_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getOosRisk(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(aiService.getOosRisk(u.getTenantId(), branchId));
    }

    @GetMapping("/instrument-trend")
    @PreAuthorize("hasAuthority('AI_INSTRUMENT_TREND_VIEW')")
    @Operation(summary = "Get instrument calibration trend",
               description = "Requires: AI_INSTRUMENT_TREND_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getInstrumentTrend(
            @RequestParam Long instrumentId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(aiService.getInstrumentTrend(u.getTenantId(), instrumentId));
    }

    @GetMapping("/workload")
    @PreAuthorize("hasAuthority('AI_WORKLOAD_VIEW')")
    @Operation(summary = "Get workload prediction for branch",
               description = "Requires: AI_WORKLOAD_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> getWorkload(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(aiService.getWorkloadPrediction(u.getTenantId(), branchId));
    }
}
