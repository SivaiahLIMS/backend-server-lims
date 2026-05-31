package com.sivayahealth.lims.dto.supplier;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AddSupplierDocumentRequest {
    private String docType;
    private Long fileId;
    private String version;
    private LocalDate expiryDate;
}
