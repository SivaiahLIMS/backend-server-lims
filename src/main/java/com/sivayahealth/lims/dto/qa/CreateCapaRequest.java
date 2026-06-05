package com.sivayahealth.lims.dto.qa;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateCapaRequest {
    private Long deviationId;
    private String title;
    private String actionDesc;
    private String priority;
    private Long ownerId;
    private LocalDate dueDate;
}
