package com.shiftsync.shiftsync.availability.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ManagerWeeklyAvailabilityResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<EmployeeWeeklyAvailability> employees
) {

    public record EmployeeWeeklyAvailability(
            Long employeeId,
            String fullName,
            List<DailyAvailability> days
    ) {
    }

    public record DailyAvailability(
            LocalDate date,
            DayOfWeek dayOfWeek,
            boolean overridden,
            List<TimeWindow> windows
    ) {
    }

    public record TimeWindow(
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
