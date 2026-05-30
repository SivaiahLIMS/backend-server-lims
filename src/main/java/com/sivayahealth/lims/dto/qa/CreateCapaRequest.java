package com.sivayahealth.lims.dto.qa;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateCapaRequest {
    private Long deviationId;
    private String actionDesc;
    private Long ownerId;
    private LocalDate dueDate;
}
