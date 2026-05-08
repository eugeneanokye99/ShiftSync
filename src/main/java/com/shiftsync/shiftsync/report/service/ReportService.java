package com.shiftsync.shiftsync.report.service;

import com.shiftsync.shiftsync.report.dto.CoverageReportEntry;
import com.shiftsync.shiftsync.report.dto.CoverageReportPageResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /**
     * Returns a paginated coverage report for a location and date range.
     */
    CoverageReportPageResponse getCoverageReport(Long locationId, LocalDate from, LocalDate to, int page, int size);

    /**
     * Returns all coverage entries for a location and date range (used for CSV export).
     */
    List<CoverageReportEntry> getAllCoverageEntries(Long locationId, LocalDate from, LocalDate to);
}
