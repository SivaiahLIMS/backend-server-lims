package com.sivayahealth.lims.dto.inventory;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateReagentRequest {
    private String name;
    private String category;
    private String formula;
    private String defaultUom;
    private BigDecimal minStockLevel;
    private BigDecimal reorderLevel;
}
