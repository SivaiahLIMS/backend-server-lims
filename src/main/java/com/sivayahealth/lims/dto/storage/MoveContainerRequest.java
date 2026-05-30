package com.sivayahealth.lims.dto.storage;

import lombok.Data;

@Data
public class MoveContainerRequest {
    private Long locationId;
    private Long userId;
    private String reason;
}
