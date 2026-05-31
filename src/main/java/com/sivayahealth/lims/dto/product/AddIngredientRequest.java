package com.sivayahealth.lims.dto.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddIngredientRequest {
    private Long ingredientId;
    private BigDecimal quantity;
    private String uom;
    private String grade;
}
