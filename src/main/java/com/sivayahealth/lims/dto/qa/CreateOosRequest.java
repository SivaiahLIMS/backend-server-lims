package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class CreateOosRequest {
    private Long branchId;
    private Long sampleId;
    private Long testId;
    /** OOS or OOT */
    private String oosType;
    private String description;
}
