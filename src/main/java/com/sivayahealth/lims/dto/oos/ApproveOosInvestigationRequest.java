package com.sivayahealth.lims.dto.oos;

import lombok.Data;

@Data
public class ApproveOosInvestigationRequest {
    private Long approverId;
    private String comment;
}
