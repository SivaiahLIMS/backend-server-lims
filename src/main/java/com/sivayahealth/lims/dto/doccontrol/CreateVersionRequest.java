package com.sivayahealth.lims.dto.doccontrol;

import lombok.Data;

@Data
public class CreateVersionRequest {
    private String content;
    private String changeSummary;
}
