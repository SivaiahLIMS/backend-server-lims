package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class CompletePhaseIIRequest {
    private String rootCause;
    private boolean capaRequired;
    private String capaDescription;
}
