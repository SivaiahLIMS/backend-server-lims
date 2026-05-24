package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.user.CreateUserRequest;
import com.sivayahealth.lims.dto.user.UserResponse;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final TenantRepository tenantRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw LimsException.conflict("Username already exists");
        }
        if (request.email() != null && userRepository.existsByEmail(request.email())) {
            throw LimsException.conflict("Email already in use");
        }

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));

        AppUser user = AppUser.builder()
                .tenant(tenant)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .email(request.email())
                .status("ACTIVE")
                .build();
        user = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .build();
        profileRepository.save(profile);

        auditService.log(tenant.getId(), null, "AppUser", user.getId(), "CREATE", null, user.getUsername());
        return UserResponse.from(user, profile);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
        return UserResponse.from(user, profile);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByTenant(Long tenantId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getTenant().getId().equals(tenantId))
                .map(u -> UserResponse.from(u, profileRepository.findByUserId(u.getId()).orElse(null)))
                .toList();
    }

    @Transactional
    public void assignRole(Long userId, Long tenantId, Long branchId, Long roleId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> LimsException.notFound("Role not found"));
        Branch branch = branchId != null ? branchRepository.findById(branchId).orElse(null) : null;

        UserRole userRole = UserRole.builder()
                .user(user).tenant(tenant).branch(branch).role(role)
                .build();
        userRoleRepository.save(userRole);
        auditService.log(tenantId, userId, "UserRole", userId, "ASSIGN_ROLE", null, role.getCode());
    }

    @Transactional
    public void lockUser(Long userId, Long tenantId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        user.setStatus("LOCKED");
        userRepository.save(user);
        auditService.log(tenantId, userId, "AppUser", userId, "LOCK", "ACTIVE", "LOCKED");
    }

    @Transactional
    public void unlockUser(Long userId, Long tenantId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        user.setStatus("ACTIVE");
        user.setFailedAttempts(0);
        user.setLockedAt(null);
        userRepository.save(user);
        auditService.log(tenantId, userId, "AppUser", userId, "UNLOCK", "LOCKED", "ACTIVE");
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword, Long tenantId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedAttempts(0);
        userRepository.save(user);
        auditService.log(tenantId, userId, "AppUser", userId, "RESET_PASSWORD", null, null);
    }
}
