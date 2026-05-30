package com.sivayahealth.lims.dto.sample;

import lombok.Data;

@Data
public class RegisterSampleRequest {
    private Long branchId;
    private String sampleNo;
    private String sampleType;
    private String productName;
    private String batchNo;
}
