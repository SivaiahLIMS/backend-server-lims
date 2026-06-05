package com.sivayahealth.lims.dto.qa;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateCapaRequest {
    private String title;
    private String actionDesc;
    private String priority;
    private LocalDate dueDate;
}
