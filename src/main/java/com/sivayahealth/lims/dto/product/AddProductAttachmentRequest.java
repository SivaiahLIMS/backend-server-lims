package com.sivayahealth.lims.dto.product;

import lombok.Data;

@Data
public class AddProductAttachmentRequest {
    private String fileName;
    private String fileType;
    private String filePath;
}
