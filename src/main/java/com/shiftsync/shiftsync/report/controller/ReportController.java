package com.shiftsync.shiftsync.report.controller;

import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.report.dto.CoverageReportEntry;
import com.shiftsync.shiftsync.report.dto.CoverageReportPageResponse;
import com.shiftsync.shiftsync.report.dto.OvertimeReportEntry;
import com.shiftsync.shiftsync.report.dto.OvertimeReportPageResponse;
import com.shiftsync.shiftsync.report.exporter.CsvExporter;
import com.shiftsync.shiftsync.report.service.ReportService;
import com.shiftsync.shiftsync.shift.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reporting endpoints for coverage, overtime, and leave utilization")
public class ReportController {

    private final ReportService reportService;
    private final ShiftService shiftService;
    private final AuthenticationHelper authenticationHelper;
    private final CsvExporter csvExporter;

    @GetMapping(value = "/coverage", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Coverage report (JSON)",
            description = "Returns paginated shift coverage for a location and date range. Managers are restricted to their assigned locations."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coverage report returned",
                    content = @Content(schema = @Schema(implementation = CoverageReportPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Manager not assigned to this location",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CoverageReportPageResponse> getCoverageReport(
            Authentication authentication,
            @RequestParam Long locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        shiftService.verifyManagerLocationAccess(actorUserId, locationId);
        return ResponseEntity.ok(reportService.getCoverageReport(locationId, from, to, page, size));
    }

    @GetMapping(value = "/coverage", produces = "text/csv")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Coverage report (CSV)",
            description = "Returns full shift coverage for a location and date range as a downloadable CSV file."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coverage CSV returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Manager not assigned to this location",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<String> getCoverageReportCsv(
            Authentication authentication,
            @RequestParam Long locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        shiftService.verifyManagerLocationAccess(actorUserId, locationId);
        List<CoverageReportEntry> entries = reportService.getAllCoverageEntries(locationId, from, to);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"coverage-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvExporter.toCoverageCsv(entries));
    }

    @GetMapping(value = "/overtime", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(
            summary = "Overtime report (JSON)",
            description = "Returns a paginated list of employees who exceeded contracted hours in the period, sorted by overage descending. locationId is optional."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overtime report returned",
                    content = @Content(schema = @Schema(implementation = OvertimeReportPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OvertimeReportPageResponse> getOvertimeReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long locationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reportService.getOvertimeReport(locationId, from, to, page, size));
    }

    @GetMapping(value = "/overtime", produces = "text/csv")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(
            summary = "Overtime report (CSV)",
            description = "Returns the full overtime report for a pay period as a downloadable CSV file."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overtime CSV returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<String> getOvertimeReportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long locationId
    ) {
        List<OvertimeReportEntry> entries = reportService.getAllOvertimeEntries(locationId, from, to);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"overtime-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvExporter.toOvertimeCsv(entries));
    }
}
