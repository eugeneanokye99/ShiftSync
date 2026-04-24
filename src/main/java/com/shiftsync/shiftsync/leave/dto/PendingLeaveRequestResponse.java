package com.shiftsync.shiftsync.leave.dto;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PendingLeaveRequestResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String department,
        LocalDate startDate,
        LocalDate endDate,
        LeaveType leaveType,
        long daysRequested,
        String reason,
        LeaveStatus status,
        LocalDateTime submittedAt
) {
}

