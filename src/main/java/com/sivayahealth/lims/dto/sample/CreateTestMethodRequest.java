package com.sivayahealth.lims.dto.sample;

import lombok.Data;

@Data
public class CreateTestMethodRequest {
    private String name;
    private String description;
    private Long sopDocumentId;
    private String version;
}
