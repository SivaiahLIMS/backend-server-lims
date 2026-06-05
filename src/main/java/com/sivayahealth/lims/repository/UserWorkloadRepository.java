package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.UserWorkload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserWorkloadRepository extends JpaRepository<UserWorkload, Long> {
    Optional<UserWorkload> findByTenantIdAndUser_Id(Long tenantId, Long userId);
    List<UserWorkload> findByTenantIdAndBranchId(Long tenantId, Long branchId);
}
