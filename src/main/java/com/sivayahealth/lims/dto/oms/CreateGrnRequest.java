package com.sivayahealth.lims.dto.oms;

import lombok.Data;

@Data
public class CreateGrnRequest {
    private Long branchId;
    private Long poId;
    private String grnNo;
}
