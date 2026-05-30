package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.user.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final TenantRepository tenantRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRolePermissionRepository tenantRolePermissionRepository;
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

    /**
     * Returns every role active in the tenant, each with its full user list.
     */
    @Transactional(readOnly = true)
    public List<RoleUserSummary> getUsersByRole(Long tenantId) {
        List<Role> roles = userRoleRepository.findDistinctRolesByTenantId(tenantId);
        Map<Long, UserProfile> profileMap = buildProfileMap(tenantId);

        return roles.stream().map(role -> {
            List<UserRole> assignments = userRoleRepository.findByTenantIdAndRoleId(tenantId, role.getId());
            List<RoleUserSummary.UserRoleEntry> users = assignments.stream()
                    .map(ur -> {
                        AppUser u = ur.getUser();
                        UserProfile p = profileMap.get(u.getId());
                        return new RoleUserSummary.UserRoleEntry(
                                u.getId(), u.getUsername(), u.getEmail(),
                                p != null ? p.getFirstName() : null,
                                p != null ? p.getLastName() : null,
                                u.getStatus(),
                                ur.getBranch() != null ? ur.getBranch().getId() : null,
                                ur.getBranch() != null ? ur.getBranch().getName() : null
                        );
                    })
                    .distinct()
                    .toList();
            return new RoleUserSummary(
                    role.getId(), role.getCode(), role.getName(), role.getDescription(),
                    users.size(), users
            );
        }).toList();
    }

    /**
     * Returns users assigned to a specific role in the tenant.
     */
    @Transactional(readOnly = true)
    public RoleUserSummary getUsersByRoleId(Long tenantId, Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> LimsException.notFound("Role not found"));
        Map<Long, UserProfile> profileMap = buildProfileMap(tenantId);

        List<UserRole> assignments = userRoleRepository.findByTenantIdAndRoleId(tenantId, roleId);
        List<RoleUserSummary.UserRoleEntry> users = assignments.stream()
                .map(ur -> {
                    AppUser u = ur.getUser();
                    UserProfile p = profileMap.get(u.getId());
                    return new RoleUserSummary.UserRoleEntry(
                            u.getId(), u.getUsername(), u.getEmail(),
                            p != null ? p.getFirstName() : null,
                            p != null ? p.getLastName() : null,
                            u.getStatus(),
                            ur.getBranch() != null ? ur.getBranch().getId() : null,
                            ur.getBranch() != null ? ur.getBranch().getName() : null
                    );
                })
                .toList();
        return new RoleUserSummary(
                role.getId(), role.getCode(), role.getName(), role.getDescription(),
                users.size(), users
        );
    }

    /**
     * Returns every permission defined for the tenant, each with users who hold it (via any role).
     */
    @Transactional(readOnly = true)
    public List<PermissionUserSummary> getUsersByPermission(Long tenantId) {
        List<Permission> allPermissions = permissionRepository.findAll();
        List<UserRole> allAssignments = userRoleRepository.findByTenantId(tenantId);
        Map<Long, UserProfile> profileMap = buildProfileMap(tenantId);

        // role → permissions granted for this tenant
        Map<Long, List<TenantRolePermission>> permsByRole = tenantRolePermissionRepository
                .findAll().stream()
                .filter(trp -> trp.getTenant().getId().equals(tenantId))
                .collect(Collectors.groupingBy(trp -> trp.getRole().getId()));

        return allPermissions.stream().map(perm -> {
            // which roles have this permission in this tenant?
            List<Long> roleIdsWithPerm = permsByRole.values().stream()
                    .flatMap(List::stream)
                    .filter(trp -> trp.getPermission().getId().equals(perm.getId()))
                    .map(trp -> trp.getRole().getId())
                    .distinct()
                    .toList();

            List<PermissionUserSummary.PermissionUserEntry> users = allAssignments.stream()
                    .filter(ur -> roleIdsWithPerm.contains(ur.getRole().getId()))
                    .map(ur -> {
                        AppUser u = ur.getUser();
                        UserProfile p = profileMap.get(u.getId());
                        return new PermissionUserSummary.PermissionUserEntry(
                                u.getId(), u.getUsername(), u.getEmail(),
                                p != null ? p.getFirstName() : null,
                                p != null ? p.getLastName() : null,
                                u.getStatus(),
                                ur.getRole().getId(), ur.getRole().getCode(), ur.getRole().getName()
                        );
                    })
                    .distinct()
                    .toList();

            return new PermissionUserSummary(
                    perm.getId(), perm.getCode(), perm.getDescription(),
                    users.size(), users
            );
        }).toList();
    }

    /**
     * Returns a full role+permission breakdown for every user in the tenant.
     */
    @Transactional(readOnly = true)
    public List<UserRolePermissionReport> getUserRolePermissionReport(Long tenantId) {
        List<UserRole> allAssignments = userRoleRepository.findByTenantId(tenantId);
        Map<Long, UserProfile> profileMap = buildProfileMap(tenantId);

        Map<Long, List<TenantRolePermission>> permsByRole = tenantRolePermissionRepository
                .findAll().stream()
                .filter(trp -> trp.getTenant().getId().equals(tenantId))
                .collect(Collectors.groupingBy(trp -> trp.getRole().getId()));

        Map<Long, List<UserRole>> assignmentsByUser = allAssignments.stream()
                .collect(Collectors.groupingBy(ur -> ur.getUser().getId()));

        return assignmentsByUser.entrySet().stream().map(entry -> {
            Long userId = entry.getKey();
            List<UserRole> userRoles = entry.getValue();
            AppUser user = userRoles.get(0).getUser();
            UserProfile profile = profileMap.get(userId);

            List<UserRolePermissionReport.RoleEntry> roleEntries = userRoles.stream().map(ur -> {
                List<String> perms = permsByRole.getOrDefault(ur.getRole().getId(), List.of())
                        .stream()
                        .map(trp -> trp.getPermission().getCode())
                        .sorted()
                        .toList();
                return new UserRolePermissionReport.RoleEntry(
                        ur.getRole().getId(), ur.getRole().getCode(), ur.getRole().getName(),
                        ur.getBranch() != null ? ur.getBranch().getId() : null,
                        ur.getBranch() != null ? ur.getBranch().getName() : null,
                        perms
                );
            }).toList();

            return new UserRolePermissionReport(
                    user.getId(), user.getUsername(), user.getEmail(),
                    profile != null ? profile.getFirstName() : null,
                    profile != null ? profile.getLastName() : null,
                    user.getStatus(),
                    roleEntries
            );
        }).toList();
    }

    private Map<Long, UserProfile> buildProfileMap(Long tenantId) {
        return profileRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
    }
}
