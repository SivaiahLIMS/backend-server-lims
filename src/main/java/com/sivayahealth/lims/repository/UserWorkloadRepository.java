package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.UserWorkload;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserWorkloadRepository extends JpaRepository<UserWorkload, Long> {
    Optional<UserWorkload> findByTenantIdAndUser_Id(Long tenantId, Long userId);
}
