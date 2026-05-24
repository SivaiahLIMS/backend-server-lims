package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.UomDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UomDetailsRepository extends JpaRepository<UomDetails, Long> {
    List<UomDetails> findByTenantIdAndActiveTrue(Long tenantId);
}
