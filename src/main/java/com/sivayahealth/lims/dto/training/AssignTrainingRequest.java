package com.sivayahealth.lims.dto.training;

import lombok.Data;

@Data
public class AssignTrainingRequest {
    private Long trainingId;
    private Long userId;
    private Long assignedById;
}
