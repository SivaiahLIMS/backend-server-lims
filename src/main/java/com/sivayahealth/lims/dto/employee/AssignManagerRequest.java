package com.sivayahealth.lims.dto.employee;

import lombok.Data;

@Data
public class AssignManagerRequest {
    private Long managerId;
    private int level = 1;
}
