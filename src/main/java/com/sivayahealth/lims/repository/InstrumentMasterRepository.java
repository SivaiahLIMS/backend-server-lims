package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstrumentMasterRepository extends JpaRepository<InstrumentMaster, Long> {
    List<InstrumentMaster> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<InstrumentMaster> findByTenantId(Long tenantId);
    boolean existsByInstrumentCode(String instrumentCode);
    java.util.Optional<InstrumentMaster> findByTenant_IdAndBarcodeValue(Long tenantId, String barcodeValue);

    /** Active (usable) instruments — status = AVAILABLE */
    List<InstrumentMaster> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);

    List<InstrumentMaster> findByTenantIdAndStatus(Long tenantId, String status);
}
