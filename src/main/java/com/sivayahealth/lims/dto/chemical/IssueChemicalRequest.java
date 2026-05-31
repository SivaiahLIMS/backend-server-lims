package com.sivayahealth.lims.dto.chemical;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IssueChemicalRequest {
    private BigDecimal quantity;
    private int containers;
    private Long issuedToId;
    private String purpose;
}
