package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

@Data
public class UpdateWorksheetRequest {
    private Long productId;
    private String batchNo;
    private Long templateId;
    private Long documentVersionId;
}
