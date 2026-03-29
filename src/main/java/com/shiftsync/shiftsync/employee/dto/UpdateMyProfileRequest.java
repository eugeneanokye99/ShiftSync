package com.shiftsync.shiftsync.employee.dto;

import com.shiftsync.shiftsync.common.enums.EmploymentType;

import java.util.List;

public record UpdateMyProfileRequest(
        String phone,
        List<String> skills,
        Boolean notificationEnabled,
        EmploymentType employmentType,
        Long departmentId,
        Long locationId
) {
}

