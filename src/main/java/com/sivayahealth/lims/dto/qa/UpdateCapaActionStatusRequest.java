package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class UpdateCapaActionStatusRequest {
    private String status;
    private String completionRemarks;
}
