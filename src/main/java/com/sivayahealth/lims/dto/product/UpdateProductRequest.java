package com.sivayahealth.lims.dto.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    private String productCode;
    private String productName;
    private String productType;
    private String strength;
    private String dosageForm;
    private BigDecimal batchSize;
    private String batchUom;
    private String hsnCode;
    private String therapeuticCategory;
    private String regulatoryStatus;
    private Integer shelfLifeValue;
    private String shelfLifeUnit;
    private String storageCondition;
    private Long manufacturerId;
    private Long siteId;
    private Long productionLineId;
    private String primaryPackaging;
    private String secondaryPackaging;
    private String samplingPlan;
    private BigDecimal sampleQuantity;
    private String sampleUom;
    private Long qcReviewerId;
    private Long qcManagerId;
}
