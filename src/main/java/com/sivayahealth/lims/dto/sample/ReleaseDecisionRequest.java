package com.sivayahealth.lims.dto.sample;

import com.sivayahealth.lims.entity.ReleaseStatus;
import lombok.Data;

@Data
public class ReleaseDecisionRequest {
    private ReleaseStatus decision;
    private String reason;
}
