package com.shiftsync.shiftsync.report.dto;

import java.util.List;

public record CoverageReportPageResponse(
        List<CoverageReportEntry> content,
        int totalElements,
        int totalPages,
        int page
) {
}
