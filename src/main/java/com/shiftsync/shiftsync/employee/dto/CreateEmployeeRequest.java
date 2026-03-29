package com.shiftsync.shiftsync.employee.dto;

import com.shiftsync.shiftsync.common.enums.EmploymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateEmployeeRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        String phone,

        @NotNull(message = "Employment type is required")
        EmploymentType employmentType,

        @NotNull(message = "Department ID is required")
        Long departmentId,

        @NotNull(message = "Location ID is required")
        Long locationId,

        List<String> skills,

        @NotNull(message = "Contracted weekly hours is required")
        @DecimalMin(value = "0.01", message = "Contracted weekly hours must be greater than 0")
        BigDecimal contractedWeeklyHours,

        LocalDate hireDate
) {
}

