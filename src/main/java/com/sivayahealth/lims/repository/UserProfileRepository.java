package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    @Query("SELECT p FROM UserProfile p WHERE p.user.tenant.id = :tenantId")
    List<UserProfile> findByTenantId(@Param("tenantId") Long tenantId);
}
