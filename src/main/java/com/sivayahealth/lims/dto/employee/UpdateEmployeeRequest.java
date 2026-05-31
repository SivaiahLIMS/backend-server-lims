package com.sivayahealth.lims.dto.employee;

import lombok.Data;

@Data
public class UpdateEmployeeRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String designation;
    private Long roleId;
    private Long managerId;
    private Long reviewerId;
}
