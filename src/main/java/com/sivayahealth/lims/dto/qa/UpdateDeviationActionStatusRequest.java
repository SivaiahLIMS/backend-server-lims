package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class UpdateDeviationActionStatusRequest {
    private String status;
    private String completionRemarks;
}
