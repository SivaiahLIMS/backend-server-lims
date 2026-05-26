package com.sivayahealth.lims.dto.chemical;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChemicalLabelDto {
    private Long registrationId;
    private String regNo;
    private String chemicalName;
    private String chemicalPrefix;
    private String casNo;
    private String lotNo;
    private String grade;
    private String category;
    private String hazardClass;
    private String quantity;
    private String uom;
    private String mfgDate;
    private String expiryDate;
    private String receivedDate;
    private String storageCondition;
    private Long tenantId;
    private String tenantName;
    private Long branchId;
    private String branchName;
    private String qrPayload;
    private String qrBase64;
}
