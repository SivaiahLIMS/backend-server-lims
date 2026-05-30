package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Employee Registration",
     description = "Employee management with hierarchy, role assignment, and audit trail. " +
                   "All endpoints are scoped by X-Branch-Id header + JWT tenant.")
public class EmployeeController {

    private final EmployeeService employeeService;

    // ── List / Lookup ─────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @Operation(summary = "List all employees for tenant + branch")
    public ResponseEntity<List<EmployeeMaster>> listAll(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(employeeService.listAll(u.getTenantId(), branchId));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @Operation(summary = "List active employees for tenant + branch")
    public ResponseEntity<List<EmployeeMaster>> listActive(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(employeeService.listActive(u.getTenantId(), branchId));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @Operation(summary = "Get a single employee by ID (tenant + branch scoped)")
    public ResponseEntity<EmployeeMaster> getById(
            @PathVariable Long employeeId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(employeeService.getById(u.getTenantId(), branchId, employeeId));
    }

    @GetMapping("/eligible-for-assignment")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @Operation(summary = "List employees eligible for worksheet assignment (active, trained)")
    public ResponseEntity<List<EmployeeMaster>> eligibleForAssignment(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(employeeService.getEligibleForAssignment(u.getTenantId(), branchId));
    }

    @GetMapping("/{managerId}/direct-reports")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @Operation(summary = "List direct reports of a manager (tenant + branch scoped)")
    public ResponseEntity<List<EmployeeMaster>> directReports(
            @PathVariable Long managerId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(employeeService.getDirectReports(u.getTenantId(), branchId, managerId));
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    @Operation(summary = "Register a new employee",
               description = "Required: employeeCode, firstName, lastName, email, designation. " +
                             "Optional: phone, roleId, managerId, reviewerId, loginUserId.")
    public ResponseEntity<EmployeeMaster> create(
            @RequestBody EmployeeMaster body,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.create(u.getTenantId(), branchId, u.getUser().getId(), body));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Update employee fields",
               description = "Pass only the fields to change: firstName, lastName, phone, designation, roleId, managerId, reviewerId.")
    public ResponseEntity<EmployeeMaster> update(
            @PathVariable Long employeeId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, Object> fields,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                employeeService.update(u.getTenantId(), branchId, employeeId, u.getUser().getId(), fields));
    }

    @PostMapping("/{employeeId}/deactivate")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Deactivate an employee")
    public ResponseEntity<EmployeeMaster> deactivate(
            @PathVariable Long employeeId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                employeeService.deactivate(u.getTenantId(), branchId, employeeId, u.getUser().getId()));
    }

    @PostMapping("/{employeeId}/activate")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Re-activate a deactivated employee")
    public ResponseEntity<EmployeeMaster> activate(
            @PathVariable Long employeeId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                employeeService.activate(u.getTenantId(), branchId, employeeId, u.getUser().getId()));
    }

    // ── Hierarchy ─────────────────────────────────────────────────────────────

    @GetMapping("/{employeeId}/hierarchy")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @Operation(summary = "Get full management hierarchy for an employee")
    public ResponseEntity<List<EmployeeHierarchy>> getHierarchy(
            @PathVariable Long employeeId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                employeeService.getHierarchy(u.getTenantId(), branchId, employeeId));
    }

    @PostMapping("/{employeeId}/hierarchy/manager")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Assign a manager to an employee",
               description = "Body: managerId (Long), level (int, 1 = direct manager).")
    public ResponseEntity<EmployeeHierarchy> assignManager(
            @PathVariable Long employeeId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        Long managerId = ((Number) body.get("managerId")).longValue();
        int level = body.containsKey("level") ? ((Number) body.get("level")).intValue() : 1;
        return ResponseEntity.status(HttpStatus.CREATED).body(
                employeeService.assignManager(u.getTenantId(), branchId, employeeId, managerId, level));
    }

    @DeleteMapping("/{employeeId}/hierarchy/manager/{managerId}")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Remove a manager relationship from an employee")
    public ResponseEntity<Void> removeManager(
            @PathVariable Long employeeId,
            @PathVariable Long managerId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        employeeService.removeManager(u.getTenantId(), branchId, employeeId, managerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{employeeId}/reviewer")
    @PreAuthorize("hasAuthority('EMPLOYEE_EDIT')")
    @Operation(summary = "Assign a reviewer to an employee",
               description = "Body: reviewerId (Long).")
    public ResponseEntity<EmployeeMaster> assignReviewer(
            @PathVariable Long employeeId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        Long reviewerId = ((Number) body.get("reviewerId")).longValue();
        return ResponseEntity.ok(
                employeeService.assignReviewer(u.getTenantId(), branchId, employeeId, reviewerId));
    }

    // ── Audit ─────────────────────────────────────────────────────────────────

    @GetMapping("/{employeeId}/audit")
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW')")
    @Operation(summary = "Field-level audit trail for an employee")
    public ResponseEntity<List<EmployeeAudit>> auditTrail(
            @PathVariable Long employeeId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                employeeService.getAuditTrail(u.getTenantId(), branchId, employeeId));
    }
}
