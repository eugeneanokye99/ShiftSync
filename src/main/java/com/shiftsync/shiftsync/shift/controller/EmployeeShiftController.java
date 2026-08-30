package com.shiftsync.shiftsync.shift.controller;

import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.shift.dto.EmployeeShiftResponse;
import com.shiftsync.shiftsync.shift.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/employees/me/shifts")
@RequiredArgsConstructor
@Tag(name = "Employee Shifts", description = "Employee shift viewing endpoints")
public class EmployeeShiftController {

    private final ShiftService shiftService;
    private final AuthenticationHelper authenticationHelper;

    @GetMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "View my upcoming shifts",
            description = "Returns assigned shifts in the given date range. Defaults to today through today + 28 days. Add ?include=cancelled to include cancelled shifts."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shifts returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Employee profile not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<EmployeeShiftResponse>> getMyShifts(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String include
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        LocalDate fromDate = from != null ? from : LocalDate.now();
        LocalDate toDate = to != null ? to : LocalDate.now().plusDays(28);
        boolean includeCancelled = "cancelled".equalsIgnoreCase(include);
        return ResponseEntity.ok(shiftService.getMyShifts(actorUserId, fromDate, toDate, includeCancelled));
    }
}
