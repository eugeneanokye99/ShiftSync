package com.shiftsync.shiftsync.employee.dto;

import com.shiftsync.shiftsync.common.enums.EmploymentType;

public record GetEmployeesRequest(
        Long actorUserId,
        boolean isManager,
        Long departmentId,
        Long locationId,
        EmploymentType employmentType,
        Boolean active,
        int page,
        int size,
        String sortBy
) {
}

