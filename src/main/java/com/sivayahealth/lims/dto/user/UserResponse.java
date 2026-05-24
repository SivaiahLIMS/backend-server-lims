package com.sivayahealth.lims.dto.user;

import com.sivayahealth.lims.entity.AppUser;
import com.sivayahealth.lims.entity.UserProfile;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    String status,
    String firstName,
    String lastName,
    String phone,
    Long tenantId,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {
    public static UserResponse from(AppUser user, UserProfile profile) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getStatus(),
            profile != null ? profile.getFirstName() : null,
            profile != null ? profile.getLastName() : null,
            profile != null ? profile.getPhone() : null,
            user.getTenant().getId(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }
}
