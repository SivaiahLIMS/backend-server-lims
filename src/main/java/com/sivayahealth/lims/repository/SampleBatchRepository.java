package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.SampleBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SampleBatchRepository extends JpaRepository<SampleBatch, Long> {
    List<SampleBatch> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    Optional<SampleBatch> findByTenantIdAndBatchNo(Long tenantId, String batchNo);
    List<SampleBatch> findByTenantIdAndProductId(Long tenantId, Long productId);
}
