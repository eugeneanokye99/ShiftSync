package com.shiftsync.shiftsync.shift.controller;

import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.shift.dto.RejectSwapRequest;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapRequest;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapResponse;
import com.shiftsync.shiftsync.shift.entity.ShiftSwapStatus;
import com.shiftsync.shiftsync.shift.service.ShiftSwapService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shift-swaps")
@RequiredArgsConstructor
@Tag(name = "Shift Swaps", description = "Shift swap request and approval endpoints")
public class ShiftSwapController {

    private final ShiftSwapService shiftSwapService;
    private final AuthenticationHelper authenticationHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Retrieve shift swap requests",
            description = "Employees see their own swaps (as requester or target), optionally filtered by status. Managers see pending swaps for their location. HR Admins see all pending swaps."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Swaps returned", content = @Content(schema = @Schema(implementation = ShiftSwapResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User or employee profile not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ShiftSwapResponse>> getMySwaps(
            Authentication authentication,
            @RequestParam(required = false) ShiftSwapStatus status
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        return ResponseEntity.ok(shiftSwapService.getMySwaps(actorUserId, status));
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "Request a shift swap",
            description = "Allows an employee to propose a shift swap with a colleague. Provides myShiftAssignmentId and targetEmployeeId; optionally targetShiftAssignmentId for a two-way swap."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Swap request created", content = @Content(schema = @Schema(implementation = ShiftSwapResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requester not assigned to the referenced shift", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Assignment or employee not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ShiftSwapResponse> requestSwap(
            Authentication authentication,
            @Valid @RequestBody ShiftSwapRequest request
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shiftSwapService.requestSwap(actorUserId, request));
    }

    @PatchMapping("/{swapId}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Approve a shift swap",
            description = "Atomically reassigns both shifts within a single transaction. Re-runs conflict checks before approval; returns 409 if a conflict is detected."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Swap approved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Swap request not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Swap not pending, or conflict detected", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> approveSwap(
            Authentication authentication,
            @PathVariable Long swapId
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        shiftSwapService.approveSwap(actorUserId, swapId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{swapId}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Reject a shift swap",
            description = "Rejects the swap request and optionally records a manager note. Returns 409 if not in PENDING_MANAGER_APPROVAL status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Swap rejected"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Swap request not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Swap is not pending approval", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> rejectSwap(
            Authentication authentication,
            @PathVariable Long swapId,
            @RequestBody(required = false) RejectSwapRequest body
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        shiftSwapService.rejectSwap(actorUserId, swapId, body != null ? body.managerNote() : null);
        return ResponseEntity.noContent().build();
    }
}
