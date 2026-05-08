package com.shiftsync.shiftsync.report.dto;

import java.util.List;

public record OvertimeReportPageResponse(
        List<OvertimeReportEntry> content,
        int totalElements,
        int totalPages,
        int currentPage
) {
}
