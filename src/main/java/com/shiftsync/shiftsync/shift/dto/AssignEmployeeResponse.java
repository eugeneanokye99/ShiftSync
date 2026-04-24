package com.shiftsync.shiftsync.shift.dto;

import java.util.List;

public record AssignEmployeeResponse(
        String status,
        List<String> conflicts,
        String message,
        Long assignmentId
) {
}

