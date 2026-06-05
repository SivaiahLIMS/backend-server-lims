package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class CreateDeviationRequest {
    private Long branchId;
    private String refEntity;
    private Long refId;
    private String title;
    private String description;
    private String severity;
    private String sourceType;
    /** CRITICAL / MAJOR / MINOR */
    private String deviationType;
}
