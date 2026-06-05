package com.sivayahealth.lims.dto.task;

import com.sivayahealth.lims.entity.TaskType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateScheduledTaskRequest {
    private TaskType taskType;
    private String title;
    private String description;
    private LocalDate dueDate;
    private String recurrenceRule;
    private Long assignedToUserId;
    private String refEntity;
    private Long refId;
}
