package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndStatus(Long userId, String status);
    List<Notification> findByTenantIdAndStatus(Long tenantId, String status);
    List<Notification> findByTenantIdAndUserId(Long tenantId, Long userId);
    List<Notification> findByTenantId(Long tenantId);
}
