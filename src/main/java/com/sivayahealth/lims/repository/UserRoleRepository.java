package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    @Query("SELECT ur.role.id FROM UserRole ur WHERE ur.user.id = :userId")
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
