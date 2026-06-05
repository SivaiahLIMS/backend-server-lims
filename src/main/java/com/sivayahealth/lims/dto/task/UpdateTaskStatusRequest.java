package com.sivayahealth.lims.dto.task;

import com.sivayahealth.lims.entity.TaskStatusEnum;
import lombok.Data;

@Data
public class UpdateTaskStatusRequest {
    private TaskStatusEnum status;
    private String resultNotes;
}
