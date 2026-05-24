package com.sivayahealth.lims.security;

import com.sivayahealth.lims.entity.AppUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class LimsUserDetails implements UserDetails {

    private final AppUser user;
    private final Long tenantId;
    private final List<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public LimsUserDetails(AppUser user, Long tenantId, List<String> permissions) {
        this.user = user;
        this.tenantId = tenantId;
        this.permissions = permissions;
        this.authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Override public String getUsername() { return user.getUsername(); }
    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return !"LOCKED".equals(user.getStatus());
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(user.getStatus());
    }
}
