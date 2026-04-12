package com.shiftsync.shiftsync.leave.dto;

import java.time.LocalDate;

public record GetPendingLeaveRequestsRequest(
        Long employeeId,
        Long locationId,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size
) {
}

