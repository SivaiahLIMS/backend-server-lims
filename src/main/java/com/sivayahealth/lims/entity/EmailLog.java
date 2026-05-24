package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "to_address", nullable = false)
    private String toAddress;

    @Column(name = "cc_address")
    private String ccAddress;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "template_key")
    private String templateKey;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_msg")
    private String errorMsg;

    @Column(name = "ref_entity")
    private String refEntity;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
