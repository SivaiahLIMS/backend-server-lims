package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.Coa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoaRepository extends JpaRepository<Coa, Long> {
    List<Coa> findBySampleId(Long sampleId);
    List<Coa> findByTenantIdAndStatus(Long tenantId, String status);
}
