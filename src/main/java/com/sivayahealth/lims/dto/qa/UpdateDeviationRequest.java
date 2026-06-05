package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class UpdateDeviationRequest {
    private String title;
    private String description;
    private String deviationType;
}
