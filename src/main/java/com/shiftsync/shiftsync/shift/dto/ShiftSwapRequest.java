package com.shiftsync.shiftsync.shift.dto;

import jakarta.validation.constraints.NotNull;

public record ShiftSwapRequest(
        @NotNull Long myShiftAssignmentId,
        @NotNull Long targetEmployeeId,
        Long targetShiftAssignmentId,
        String reason
) {}
