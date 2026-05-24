package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUser_Id(Long userId);
    List<UserSkill> findByTenantIdAndUser_Id(Long tenantId, Long userId);
}
