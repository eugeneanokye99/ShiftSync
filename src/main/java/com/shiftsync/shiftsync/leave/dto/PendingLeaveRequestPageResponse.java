package com.shiftsync.shiftsync.leave.dto;

import java.util.List;

public record PendingLeaveRequestPageResponse(
        List<PendingLeaveRequestResponse> content,
        long totalElements,
        int totalPages,
        int currentPage
) {
}

