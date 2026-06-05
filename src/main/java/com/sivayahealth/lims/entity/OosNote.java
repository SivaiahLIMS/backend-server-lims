package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "oos_note")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OosNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oos_case_id", nullable = false)
    private OosCase oosCase;

    @Column(name = "note_type", nullable = false, length = 50)
    private String noteType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
