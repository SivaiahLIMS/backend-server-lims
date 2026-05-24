package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.auth.LoginRequest;
import com.sivayahealth.lims.dto.auth.LoginResponse;
import com.sivayahealth.lims.entity.AppUser;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.AppUserRepository;
import com.sivayahealth.lims.security.JwtTokenProvider;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.security.LimsUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AppUserRepository userRepository;
    private final LimsUserDetailsService userDetailsService;
    private final AuditService auditService;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if ("LOCKED".equals(user.getStatus())) {
            throw new LockedException("Account is locked due to too many failed attempts or inactivity");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new DisabledException("Account is not active");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            user.setFailedAttempts(0);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            LimsUserDetails userDetails = (LimsUserDetails) auth.getPrincipal();
            Long branchId = request.branchId();
            String token = tokenProvider.generateToken(userDetails, branchId);
            String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

            auditService.log(user.getTenant().getId(), user.getId(), "AppUser", user.getId(), "LOGIN", null, null);

            LoginResponse response = new LoginResponse(token, refreshToken, user.getId(), user.getUsername(),
                    user.getTenant().getId(), branchId, userDetails.getPermissions());
            return response;

        } catch (BadCredentialsException ex) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= maxFailedAttempts) {
                user.setStatus("LOCKED");
                user.setLockedAt(LocalDateTime.now());
                log.warn("User {} locked after {} failed attempts", user.getUsername(), attempts);
            }
            userRepository.save(user);
            throw ex;
        }
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw LimsException.badRequest("Invalid or expired refresh token");
        }
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        LimsUserDetails userDetails = (LimsUserDetails) userDetailsService.loadUserByUsername(username);
        String newToken = tokenProvider.generateToken(userDetails, null);
        String newRefresh = tokenProvider.generateRefreshToken(username);

        return new LoginResponse(newToken, newRefresh, userDetails.getUser().getId(),
                username, userDetails.getTenantId(), null, userDetails.getPermissions());
    }

    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user ->
            auditService.log(user.getTenant().getId(), user.getId(), "AppUser", user.getId(), "LOGOUT", null, null)
        );
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, oldPassword));
        LimsUserDetails userDetails = (LimsUserDetails) auth.getPrincipal();
        AppUser user = userDetails.getUser();
        // password encoding done in UserService
        auditService.log(user.getTenant().getId(), user.getId(), "AppUser", user.getId(), "CHANGE_PASSWORD", null, null);
    }
}
