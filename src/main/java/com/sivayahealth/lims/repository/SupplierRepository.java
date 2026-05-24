package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByTenantId(Long tenantId);
    List<Supplier> findByTenantIdAndStatus(Long tenantId, String status);
}
