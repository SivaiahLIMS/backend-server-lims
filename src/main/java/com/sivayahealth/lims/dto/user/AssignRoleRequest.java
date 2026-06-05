package com.sivayahealth.lims.dto.user;

import lombok.Data;

@Data
public class AssignRoleRequest {
    private Long tenantId;
    private Long branchId;
    private Long roleId;
}
