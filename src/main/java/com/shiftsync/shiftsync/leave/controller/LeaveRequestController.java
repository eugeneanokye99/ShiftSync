package com.shiftsync.shiftsync.leave.controller;

import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.leave.dto.ApproveLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.CreateLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.GetPendingLeaveRequestsRequest;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestPageResponse;
import com.shiftsync.shiftsync.leave.dto.RejectLeaveRequest;
import com.shiftsync.shiftsync.leave.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/leave-requests")
@RequiredArgsConstructor
@Tag(name = "Leave Requests", description = "Leave request management endpoints")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final AuthenticationHelper authenticationHelper;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "Submit leave request",
            description = "Creates a leave request in PENDING status for the authenticated employee."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Leave request submitted", content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Overlapping leave request", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
            Authentication authentication,
            @Valid @RequestBody CreateLeaveRequest request
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(actorUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(
            summary = "Get pending leave requests",
            description = "Returns paginated pending leave requests with optional employee, location, and date-range filters."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pending leave requests retrieved", content = @Content(schema = @Schema(implementation = PendingLeaveRequestPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PendingLeaveRequestPageResponse> getPendingLeaveRequests(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        GetPendingLeaveRequestsRequest request = new GetPendingLeaveRequestsRequest(
                employeeId,
                locationId,
                startDate,
                endDate,
                page,
                size
        );
        return ResponseEntity.ok(leaveRequestService.getPendingLeaveRequests(request));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(
            summary = "Approve leave request",
            description = "Approves a pending leave request, stores HR note, and blocks employee availability for the leave period."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leave request approved", content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Leave request not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Leave request is not pending", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponse> approveLeaveRequest(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ApproveLeaveRequest request
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        LeaveRequestResponse response = leaveRequestService.approveLeaveRequest(actorUserId, id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(
            summary = "Reject leave request",
            description = "Rejects a pending leave request and stores the HR note."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leave request rejected", content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Leave request not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Leave request is not pending", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponse> rejectLeaveRequest(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RejectLeaveRequest request
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        LeaveRequestResponse response = leaveRequestService.rejectLeaveRequest(actorUserId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "Cancel pending leave request",
            description = "Cancels the authenticated employee's own pending leave request."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leave request cancelled", content = @Content(schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Leave request not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Leave request cannot be cancelled", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LeaveRequestResponse> cancelLeaveRequest(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        LeaveRequestResponse response = leaveRequestService.cancelLeaveRequest(actorUserId, id);
        return ResponseEntity.ok(response);
    }
}

