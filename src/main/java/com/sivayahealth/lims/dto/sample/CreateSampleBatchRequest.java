package com.sivayahealth.lims.dto.sample;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateSampleBatchRequest {
    private Long branchId;
    private Long productId;
    private String batchNo;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
}
