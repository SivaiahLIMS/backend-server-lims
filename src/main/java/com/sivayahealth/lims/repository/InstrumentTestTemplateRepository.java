package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.InstrumentTestTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InstrumentTestTemplateRepository extends JpaRepository<InstrumentTestTemplate, Long> {
    List<InstrumentTestTemplate> findByTenantIdAndActiveTrue(Long tenantId);
    List<InstrumentTestTemplate> findByInstrumentCategoryIdAndActiveTrue(Long categoryId);
}
