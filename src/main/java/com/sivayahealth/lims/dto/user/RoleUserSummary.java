package com.sivayahealth.lims.dto.user;

import java.util.List;

public record RoleUserSummary(
    Long roleId,
    String roleCode,
    String roleName,
    String roleDescription,
    int userCount,
    List<UserRoleEntry> users
) {
    public record UserRoleEntry(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String status,
        Long branchId,
        String branchName
    ) {}
}
