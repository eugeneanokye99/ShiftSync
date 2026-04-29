package com.shiftsync.shiftsync.shift.dto;

import com.shiftsync.shiftsync.shift.entity.StaffingStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record LocationShiftResponse(
        Long shiftId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String departmentName,
        Integer minimumHeadcount,
        Integer assignedCount,
        StaffingStatus staffingStatus,
        List<AssigneeInfo> assignedEmployees
) {
}
