package com.sivayahealth.lims.dto.auth;

import java.util.List;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long userId,
    String username,
    Long tenantId,
    Long branchId,
    List<String> permissions
) {
    public LoginResponse(String accessToken, String refreshToken, Long userId,
                         String username, Long tenantId, Long branchId, List<String> permissions) {
        this(accessToken, refreshToken, "Bearer", userId, username, tenantId, branchId, permissions);
    }
}
