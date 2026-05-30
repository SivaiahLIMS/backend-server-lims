package com.sivayahealth.lims.dto.user;

import java.util.List;

public record UserRolePermissionReport(
    Long userId,
    String username,
    String email,
    String firstName,
    String lastName,
    String status,
    List<RoleEntry> roles
) {
    public record RoleEntry(
        Long roleId,
        String roleCode,
        String roleName,
        Long branchId,
        String branchName,
        List<String> permissions
    ) {}
}
