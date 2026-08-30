package com.shiftsync.shiftsync.availability.dto;

import java.time.LocalDate;

public record AvailabilityOverrideResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
}

