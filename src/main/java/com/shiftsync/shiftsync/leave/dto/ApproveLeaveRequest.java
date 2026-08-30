package com.shiftsync.shiftsync.leave.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveLeaveRequest(
        @NotBlank(message = "HR note is required")
        String hrNote
) {
}

