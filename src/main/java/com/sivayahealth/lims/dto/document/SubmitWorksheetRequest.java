package com.sivayahealth.lims.dto.document;

import lombok.Data;

@Data
public class SubmitWorksheetRequest {
    private Long sampleId;
    private String filledJson;
}
