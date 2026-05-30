package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.user.*;
import com.sivayahealth.lims.entity.UserSkill;
import com.sivayahealth.lims.entity.UserWorkload;
import com.sivayahealth.lims.repository.UserSkillRepository;
import com.sivayahealth.lims.repository.UserWorkloadRepository;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User CRUD and role assignment")
public class UserController {

    private final UserService userService;
    private final UserSkillRepository userSkillRepository;
    private final UserWorkloadRepository userWorkloadRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Get all users for a tenant")
    public ResponseEntity<List<UserResponse>> getUsersByTenant(@PathVariable Long tenantId) {
        return ResponseEntity.ok(userService.getUsersByTenant(tenantId));
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('USER_ASSIGN_ROLE')")
    @Operation(summary = "Assign a role to a user")
    public ResponseEntity<Void> assignRole(
            @PathVariable Long userId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal LimsUserDetails userDetails) {
        userService.assignRole(userId,
                body.getOrDefault("tenantId", userDetails.getTenantId()),
                body.get("branchId"),
                body.get("roleId"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @Operation(summary = "Lock a user account")
    public ResponseEntity<Void> lockUser(@PathVariable Long userId,
                                          @AuthenticationPrincipal LimsUserDetails userDetails) {
        userService.lockUser(userId, userDetails.getTenantId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @Operation(summary = "Unlock a user account")
    public ResponseEntity<Void> unlockUser(@PathVariable Long userId,
                                            @AuthenticationPrincipal LimsUserDetails userDetails) {
        userService.unlockUser(userId, userDetails.getTenantId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    @Operation(summary = "Reset user password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long userId,
                                               @RequestBody Map<String, String> body,
                                               @AuthenticationPrincipal LimsUserDetails userDetails) {
        userService.resetPassword(userId, body.get("newPassword"), userDetails.getTenantId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/skills")
    @Operation(summary = "Get skills for a user")
    public ResponseEntity<List<UserSkill>> getUserSkills(@PathVariable Long userId,
                                                          @AuthenticationPrincipal LimsUserDetails userDetails) {
        return ResponseEntity.ok(userSkillRepository.findByTenantIdAndUser_Id(userDetails.getTenantId(), userId));
    }

    @PostMapping("/{userId}/skills")
    @Operation(summary = "Add or update a skill for a user")
    public ResponseEntity<UserSkill> addUserSkill(@PathVariable Long userId,
                                                    @RequestBody UserSkill skill,
                                                    @AuthenticationPrincipal LimsUserDetails userDetails) {
        skill.setTenantId(userDetails.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(userSkillRepository.save(skill));
    }

    @GetMapping("/{userId}/workload")
    @Operation(summary = "Get workload for a user")
    public ResponseEntity<Optional<UserWorkload>> getUserWorkload(@PathVariable Long userId,
                                                                   @AuthenticationPrincipal LimsUserDetails userDetails) {
        return ResponseEntity.ok(userWorkloadRepository.findByTenantIdAndUser_Id(userDetails.getTenantId(), userId));
    }

    // ── Role & Permission Reports ────────────────────────────────────────────

    @GetMapping("/reports/by-role")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "List all roles with their assigned users for the tenant")
    public ResponseEntity<List<RoleUserSummary>> getUsersByRole(
            @AuthenticationPrincipal LimsUserDetails userDetails) {
        return ResponseEntity.ok(userService.getUsersByRole(userDetails.getTenantId()));
    }

    @GetMapping("/reports/by-role/{roleId}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "List users assigned to a specific role in the tenant")
    public ResponseEntity<RoleUserSummary> getUsersByRoleId(
            @PathVariable Long roleId,
            @AuthenticationPrincipal LimsUserDetails userDetails) {
        return ResponseEntity.ok(userService.getUsersByRoleId(userDetails.getTenantId(), roleId));
    }

    @GetMapping("/reports/by-permission")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "List all permissions with users who hold each permission (via any role)")
    public ResponseEntity<List<PermissionUserSummary>> getUsersByPermission(
            @AuthenticationPrincipal LimsUserDetails userDetails) {
        return ResponseEntity.ok(userService.getUsersByPermission(userDetails.getTenantId()));
    }

    @GetMapping("/reports/full")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Full report: each user with all their roles and permissions in the tenant")
    public ResponseEntity<List<UserRolePermissionReport>> getFullReport(
            @AuthenticationPrincipal LimsUserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserRolePermissionReport(userDetails.getTenantId()));
    }
}
