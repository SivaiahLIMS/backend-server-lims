package com.sivayahealth.lims.security;

import com.sivayahealth.lims.entity.AppUser;
import com.sivayahealth.lims.repository.AppUserRepository;
import com.sivayahealth.lims.repository.TenantRolePermissionRepository;
import com.sivayahealth.lims.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LimsUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantRolePermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<Long> roleIds = userRoleRepository.findRoleIdsByUserId(user.getId());
        List<String> permissions = permissionRepository
                .findPermissionCodesByTenantAndRoles(user.getTenant().getId(), roleIds);

        return new LimsUserDetails(user, user.getTenant().getId(), permissions);
    }
}
