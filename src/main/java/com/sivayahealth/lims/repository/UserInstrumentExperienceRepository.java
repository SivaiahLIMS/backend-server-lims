package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.UserInstrumentExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserInstrumentExperienceRepository extends JpaRepository<UserInstrumentExperience, Long> {
    List<UserInstrumentExperience> findByUser_Id(Long userId);
    List<UserInstrumentExperience> findByTenantIdAndUser_Id(Long tenantId, Long userId);
}
