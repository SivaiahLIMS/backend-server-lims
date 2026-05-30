package com.sivayahealth.lims.dto.ai;

import lombok.Data;

@Data
public class AutoInitiateForecastRequest {
    private Long branchId;
    private String itemType;
    private Long itemId;
}
