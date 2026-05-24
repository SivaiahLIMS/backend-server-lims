package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.AppUser;
import com.sivayahealth.lims.entity.AuditLog;
import com.sivayahealth.lims.entity.Tenant;
import com.sivayahealth.lims.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long tenantId, Long userId, String entityType, Long entityId,
                    String action, String oldValue, String newValue) {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        AppUser user = null;
        if (userId != null) {
            user = new AppUser();
            user.setId(userId);
        }

        AuditLog log = AuditLog.builder()
                .tenant(tenant)
                .user(user)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAuditTrail(Long tenantId, String entityType, Long entityId) {
        return auditLogRepository.findByTenantIdAndEntityTypeAndEntityId(tenantId, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getTenantAuditTrail(Long tenantId) {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}
