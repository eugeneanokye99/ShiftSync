package com.shiftsync.shiftsync.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RecurringAvailabilityResponse(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}

