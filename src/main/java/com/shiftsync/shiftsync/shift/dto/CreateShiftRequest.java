package com.shiftsync.shiftsync.shift.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateShiftRequest(
        @NotNull(message = "locationId is required")
        Long locationId,

        @NotNull(message = "departmentId is required")
        Long departmentId,

        @NotNull(message = "date is required")
        @FutureOrPresent(message = "Shift date cannot be in the past")
        LocalDate date,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "endTime is required")
        LocalTime endTime,

        String requiredSkill,

        @NotNull(message = "minimumHeadcount is required")
        @Min(value = 1, message = "minimumHeadcount must be at least 1")
        Integer minimumHeadcount
) {
}
