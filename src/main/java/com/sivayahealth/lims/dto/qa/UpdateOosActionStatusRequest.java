package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class UpdateOosActionStatusRequest {
    private String status;
    private String completionRemarks;
}
