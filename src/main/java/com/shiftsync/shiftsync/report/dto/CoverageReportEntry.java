package com.shiftsync.shiftsync.report.dto;

import com.shiftsync.shiftsync.shift.entity.StaffingStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CoverageReportEntry(
        Long shiftId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String departmentName,
        Integer requiredHeadcount,
        Integer assignedCount,
        StaffingStatus staffingStatus,
        List<String> employeeNames
) {
}
