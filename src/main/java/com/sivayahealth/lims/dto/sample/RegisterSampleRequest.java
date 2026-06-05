package com.sivayahealth.lims.dto.sample;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RegisterSampleRequest {
    private Long branchId;
    private String sampleNo;
    private String sampleCode;
    private Long sampleTypeId;
    private String sampleType;
    private Long productId;
    private String productName;
    private String batchNo;
    private Long sampleBatchId;
    private BigDecimal quantity;
    private String unit;
    private LocalDateTime dueDate;
    private Integer priority;
    private Long storageLocationId;
}
