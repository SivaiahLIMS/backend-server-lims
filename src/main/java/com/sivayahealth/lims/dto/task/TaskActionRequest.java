package com.sivayahealth.lims.dto.task;

import lombok.Data;

@Data
public class TaskActionRequest {
    /** userId of the person performing the action */
    private Long userId;
    private String comment;
}
