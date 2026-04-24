package com.shiftsync.shiftsync.leave.dto;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;

import java.time.LocalDate;

public record GetLeaveRequestsRequest(
        Long employeeId,
        Long locationId,
        LeaveStatus status,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size
) {
}