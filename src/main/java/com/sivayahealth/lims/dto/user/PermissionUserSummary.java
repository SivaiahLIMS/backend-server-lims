package com.sivayahealth.lims.dto.user;

import java.util.List;

public record PermissionUserSummary(
    Long permissionId,
    String permissionCode,
    String permissionDescription,
    int userCount,
    List<PermissionUserEntry> users
) {
    public record PermissionUserEntry(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String status,
        Long roleId,
        String roleCode,
        String roleName
    ) {}
}
