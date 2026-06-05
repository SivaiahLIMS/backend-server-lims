package com.sivayahealth.lims.dto.sample;

import lombok.Data;
import java.util.List;

@Data
public class CreateSampleTypeRequest {
    private String name;
    private String description;
    private List<Long> defaultTestMethodIds;
}
