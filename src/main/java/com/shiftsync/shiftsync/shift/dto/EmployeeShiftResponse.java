package com.shiftsync.shiftsync.shift.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EmployeeShiftResponse(
        Long shiftId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String locationName,
        String departmentName,
        String status,
        List<String> assignedColleagues
) {
}
