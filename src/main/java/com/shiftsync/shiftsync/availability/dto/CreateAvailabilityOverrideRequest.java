package com.shiftsync.shiftsync.availability.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAvailabilityOverrideRequest(
        @NotNull(message = "startDate is required")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        String reason
) {
}

