package com.sivayahealth.lims.dto.oos;

import lombok.Data;

@Data
public class InitiateOosInvestigationRequest {
    private Long assigneeId;
    private String description;
    private Long requestedById;
}
