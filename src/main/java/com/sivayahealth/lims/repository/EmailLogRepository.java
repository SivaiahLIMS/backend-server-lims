package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<EmailLog> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);
    List<EmailLog> findByRefEntityAndRefId(String refEntity, Long refId);
}
