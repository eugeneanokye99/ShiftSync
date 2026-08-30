package com.shiftsync.shiftsync.shift.dto;

import jakarta.validation.constraints.NotNull;

public record AssignEmployeeRequest(
        @NotNull(message = "employeeId is required")
        Long employeeId
) {
}

