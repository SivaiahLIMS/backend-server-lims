package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.EmployeeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeMasterRepository extends JpaRepository<EmployeeMaster, Long> {

    List<EmployeeMaster> findByTenantIdAndBranchId(Long tenantId, Long branchId);

    List<EmployeeMaster> findByTenantIdAndBranchIdAndIsActive(
            Long tenantId, Long branchId, boolean isActive);

    Optional<EmployeeMaster> findByLoginUser_IdAndTenant_Id(Long userId, Long tenantId);

    boolean existsByTenant_IdAndEmployeeCode(Long tenantId, String employeeCode);

    boolean existsByTenant_IdAndEmail(Long tenantId, String email);

    /** Employees eligible for worksheet assignment in a branch */
    @Query("""
        SELECT e FROM EmployeeMaster e
        WHERE e.tenant.id = :tenantId
          AND e.branch.id = :branchId
          AND e.isActive = true
          AND e.role.code IN ('ANALYST','QC_ANALYST','REVIEWER','STOREKEEPER')
        ORDER BY e.firstName ASC
        """)
    List<EmployeeMaster> findEligibleForAssignment(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId);

    /** All direct reports of a manager within a branch */
    @Query("""
        SELECT e FROM EmployeeMaster e
        WHERE e.tenant.id = :tenantId
          AND e.branch.id = :branchId
          AND e.manager.employeeId = :managerId
        ORDER BY e.firstName ASC
        """)
    List<EmployeeMaster> findDirectReports(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("managerId") Long managerId);
}
