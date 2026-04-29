package com.shiftsync.shiftsync.shift.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ShiftSwapResponse(
        Long id,
        Long requesterId,
        String requesterName,
        LocalDate requesterShiftDate,
        LocalTime requesterStartTime,
        LocalTime requesterEndTime,
        Long targetEmployeeId,
        String targetEmployeeName,
        LocalDate targetShiftDate,
        LocalTime targetStartTime,
        LocalTime targetEndTime,
        String status,
        String reason,
        String managerNote,
        LocalDateTime createdAt
) {}
