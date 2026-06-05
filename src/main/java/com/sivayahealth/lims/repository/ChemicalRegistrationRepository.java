package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ChemicalRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChemicalRegistrationRepository extends JpaRepository<ChemicalRegistration, Long> {
    Optional<ChemicalRegistration> findByRegNo(String regNo);
    List<ChemicalRegistration> findByTenantIdAndBranchId(Long tenantId, Long branchId);

    @Query("SELECT r FROM ChemicalRegistration r WHERE r.tenant.id = :tenantId AND r.expiryDate <= :date AND r.status = 'ACTIVE'")
    List<ChemicalRegistration> findExpiringChemicals(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT r FROM ChemicalRegistration r WHERE r.expiryDate <= :date AND r.status = 'ACTIVE'")
    List<ChemicalRegistration> findExpiringAllTenants(@Param("date") LocalDate date);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(reg_no, 3) AS BIGINT)), 0) FROM chemical_registration WHERE tenant_id = :tenantId", nativeQuery = true)
    Long findMaxRegNoSeq(@Param("tenantId") Long tenantId);
}
