package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ReagentPreparation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReagentPreparationRepository extends JpaRepository<ReagentPreparation, Long> {
    List<ReagentPreparation> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<ReagentPreparation> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    List<ReagentPreparation> findByRegistrationId(Long registrationId);
    List<ReagentPreparation> findByStatusAndExpiryDateBefore(String status, LocalDate date);
}
