package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class CreateOosRequest {
    private Long branchId;
    private Long sampleId;
    private Long testId;
    private String description;
}
