package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class CreateDeviationRequest {
    private Long branchId;
    private String refEntity;
    private Long refId;
    private String description;
    /** CRITICAL / MAJOR / MINOR */
    private String severity;
}
