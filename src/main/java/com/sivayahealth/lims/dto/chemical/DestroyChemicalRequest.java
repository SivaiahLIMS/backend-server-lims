package com.sivayahealth.lims.dto.chemical;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DestroyChemicalRequest {
    private BigDecimal quantity;
    private int containers;
    private Long witnessedById;
    private String method;
    private String remarks;
}
