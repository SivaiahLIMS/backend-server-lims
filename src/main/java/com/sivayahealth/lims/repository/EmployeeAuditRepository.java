package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.EmployeeAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeAuditRepository extends JpaRepository<EmployeeAudit, Long> {
    List<EmployeeAudit> findByEmployee_EmployeeIdAndTenantIdAndBranchIdOrderByChangedAtDesc(
            Long employeeId, Long tenantId, Long branchId);
}
