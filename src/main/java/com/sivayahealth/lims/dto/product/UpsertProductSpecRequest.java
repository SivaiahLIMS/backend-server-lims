package com.sivayahealth.lims.dto.product;

import lombok.Data;

@Data
public class UpsertProductSpecRequest {
    private String specDocumentPath;
    private String testMethods;
    private String releaseCriteria;
    private String stabilityRequirements;
}
