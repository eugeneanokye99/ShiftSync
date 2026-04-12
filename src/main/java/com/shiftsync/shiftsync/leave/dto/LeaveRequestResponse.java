package com.shiftsync.shiftsync.leave.dto;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
        Long id,
        Long employeeId,
        LocalDate startDate,
        LocalDate endDate,
        LeaveType leaveType,
        String reason,
        LeaveStatus status,
        LocalDateTime submittedAt
) {
}

