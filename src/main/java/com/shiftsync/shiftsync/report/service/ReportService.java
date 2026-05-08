package com.shiftsync.shiftsync.report.service;

import com.shiftsync.shiftsync.report.dto.CoverageReportEntry;
import com.shiftsync.shiftsync.report.dto.CoverageReportPageResponse;
import com.shiftsync.shiftsync.report.dto.OvertimeReportEntry;
import com.shiftsync.shiftsync.report.dto.OvertimeReportPageResponse;

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

    /**
     * Returns a paginated overtime report for a pay period, optionally filtered by location.
     */
    OvertimeReportPageResponse getOvertimeReport(Long locationId, LocalDate from, LocalDate to, int page, int size);

    /**
     * Returns all overtime entries for a pay period (used for CSV export).
     */
    List<OvertimeReportEntry> getAllOvertimeEntries(Long locationId, LocalDate from, LocalDate to);
}
