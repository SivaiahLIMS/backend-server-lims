package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.SampleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SampleTypeRepository extends JpaRepository<SampleType, Long> {
    List<SampleType> findByTenantIdAndActiveTrue(Long tenantId);
    Optional<SampleType> findByTenantIdAndName(Long tenantId, String name);
}
