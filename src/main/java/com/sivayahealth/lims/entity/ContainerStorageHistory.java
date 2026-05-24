package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "container_storage_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContainerStorageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "container_id", nullable = false)
    private Long containerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_location_id")
    private StorageLocation fromLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_location_id")
    private StorageLocation toLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moved_by")
    private AppUser movedBy;

    @Column(name = "moved_at", nullable = false)
    private LocalDateTime movedAt = LocalDateTime.now();

    private String reason;
}
