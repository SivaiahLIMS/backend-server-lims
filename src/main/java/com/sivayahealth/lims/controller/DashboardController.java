package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard", description = "Role-based dashboard data and widgets")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/widgets")
    @PreAuthorize("hasAnyAuthority('WIDGET_CRITICAL_ALERTS','WIDGET_WORKLOAD','WIDGET_LOW_STOCK','WIDGET_CALIBRATION_DUE','WIDGET_OOS','WIDGET_EXECUTIVE_KPI')")
    @Operation(summary = "Get dashboard widgets data",
               description = "Scoped by X-Branch-Id header.")
    public ResponseEntity<Map<String, Object>> getWidgets(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(dashboardService.getWidgets(u.getTenantId(), branchId));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('WIDGET_CRITICAL_ALERTS','WIDGET_WORKLOAD','WIDGET_LOW_STOCK','WIDGET_CALIBRATION_DUE','WIDGET_OOS','WIDGET_EXECUTIVE_KPI')")
    @Operation(summary = "Get full dashboard summary",
               description = "Scoped by X-Branch-Id header.")
    public ResponseEntity<Map<String, Object>> getSummary(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(dashboardService.getDashboardData(u.getTenantId(), branchId));
    }
}
