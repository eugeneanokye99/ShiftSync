package com.shiftsync.shiftsync.employee.dto;

import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmployeeResponse(
        Long employeeId,
        Long userId,
        String fullName,
        UserRole role,
        String phone,
        EmploymentType employmentType,
        Long departmentId,
        String departmentName,
        Long locationId,
        String locationName,
        List<String> skills,
        BigDecimal contractedWeeklyHours,
        LocalDate hireDate,
        Boolean active
) {
}

