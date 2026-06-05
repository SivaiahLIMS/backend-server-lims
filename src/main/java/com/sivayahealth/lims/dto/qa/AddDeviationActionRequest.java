package com.sivayahealth.lims.dto.qa;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AddDeviationActionRequest {
    private String description;
    private Long assignedTo;
    private LocalDateTime dueDate;
}
