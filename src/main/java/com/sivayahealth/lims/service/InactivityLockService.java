package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.AppUser;
import com.sivayahealth.lims.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InactivityLockService {

    private final AppUserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.security.lock-duration-months}")
    private int inactivityMonths;

    // Run daily at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void lockInactiveUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(inactivityMonths);
        List<AppUser> inactiveUsers = userRepository.findInactiveUsers(cutoff);

        for (AppUser user : inactiveUsers) {
            user.setStatus("LOCKED");
            user.setLockedAt(LocalDateTime.now());
            userRepository.save(user);
            auditService.log(user.getTenant().getId(), user.getId(), "AppUser",
                    user.getId(), "INACTIVITY_LOCK", "ACTIVE", "LOCKED");
            log.info("User {} locked due to inactivity (last login: {})", user.getUsername(), user.getLastLoginAt());
        }

        if (!inactiveUsers.isEmpty()) {
            log.info("Locked {} users due to inactivity", inactiveUsers.size());
        }
    }
}
