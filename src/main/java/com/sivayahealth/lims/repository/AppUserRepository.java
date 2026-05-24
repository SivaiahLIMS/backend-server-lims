package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM AppUser u WHERE u.status = 'ACTIVE' AND u.lastLoginAt < :cutoff")
    List<AppUser> findInactiveUsers(LocalDateTime cutoff);
}
