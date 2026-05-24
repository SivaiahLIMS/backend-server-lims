package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "storage_location")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StorageLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private String code;

    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "is_reserved_zone", nullable = false)
    private boolean reservedZone = false;

    @Column(name = "temp_min")
    private BigDecimal tempMin;

    @Column(name = "temp_max")
    private BigDecimal tempMax;

    @Column(name = "humidity_min")
    private BigDecimal humidityMin;

    @Column(name = "humidity_max")
    private BigDecimal humidityMax;

    private Integer capacity;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
