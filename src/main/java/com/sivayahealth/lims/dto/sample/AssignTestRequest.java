package com.sivayahealth.lims.dto.sample;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AssignTestRequest {
    private Long testDefId;
    private Long testMethodId;
    private Long assignedToId;
    private LocalDateTime dueDate;
}
