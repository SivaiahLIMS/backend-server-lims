package com.sivayahealth.lims.dto.qa;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AddCapaActionRequest {
    private String description;
    private Long assignedTo;
    private LocalDateTime dueDate;
}
