package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.EmployeeHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeHierarchyRepository extends JpaRepository<EmployeeHierarchy, Long> {
    List<EmployeeHierarchy> findByEmployee_EmployeeIdAndTenantIdAndBranchId(
            Long employeeId, Long tenantId, Long branchId);
    List<EmployeeHierarchy> findByManager_EmployeeIdAndTenantIdAndBranchId(
            Long managerId, Long tenantId, Long branchId);
    void deleteByEmployee_EmployeeIdAndManager_EmployeeId(Long employeeId, Long managerId);
}
