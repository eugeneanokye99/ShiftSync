package com.shiftsync.shiftsync.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record OvertimeShiftEntry(
        Long shiftId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String locationName,
        String departmentName,
        BigDecimal hoursWorked
) {
}
