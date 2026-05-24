package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_parsed_json")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentParsedJson {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentMaster document;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "parsed_json", nullable = false, columnDefinition = "JSONB")
    private String parsedJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
