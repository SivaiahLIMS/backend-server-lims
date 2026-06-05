package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "oos_audit_trail")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OosAuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oos_case_id", nullable = false)
    private OosCase oosCase;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "old_value_json", columnDefinition = "TEXT")
    private String oldValueJson;

    @Column(name = "new_value_json", columnDefinition = "TEXT")
    private String newValueJson;

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
}
