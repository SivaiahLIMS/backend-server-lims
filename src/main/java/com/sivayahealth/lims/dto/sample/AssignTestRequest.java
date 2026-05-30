package com.sivayahealth.lims.dto.sample;

import lombok.Data;

@Data
public class AssignTestRequest {
    private Long testDefId;
    private Long assignedToId;
}
