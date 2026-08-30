package com.shiftsync.shiftsync.shift.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ShiftResponse(
        Long id,
        Long locationId,
        String locationName,
        Long departmentId,
        String departmentName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String requiredSkill,
        Integer minimumHeadcount,
        String status,
        Integer assignedCount
) {
}
