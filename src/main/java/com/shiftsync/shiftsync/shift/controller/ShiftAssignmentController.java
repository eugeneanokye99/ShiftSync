package com.shiftsync.shiftsync.shift.controller;

import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeRequest;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeResponse;
import com.shiftsync.shiftsync.shift.service.ShiftAssignmentService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
@Tag(name = "Shift Assignments", description = "Shift assignment endpoints")
public class ShiftAssignmentController {

    private final ShiftAssignmentService shiftAssignmentService;
    private final AuthenticationHelper authenticationHelper;

    @PostMapping("/{shiftId}/assignments")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Assign employee to shift",
            description = "Returns WARNING for availability mismatch unless override=true is provided."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Warning returned for availability mismatch", content = @Content(schema = @Schema(implementation = AssignEmployeeResponse.class))),
            @ApiResponse(responseCode = "201", description = "Assignment created", content = @Content(schema = @Schema(implementation = AssignEmployeeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Shift or employee not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state (duplicate assignment or cancelled shift)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssignEmployeeResponse> assignEmployee(
            Authentication authentication,
            @PathVariable Long shiftId,
            @Valid @RequestBody AssignEmployeeRequest request,
            @RequestParam(defaultValue = "false") boolean override
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        AssignEmployeeResponse response = shiftAssignmentService.assignEmployee(actorUserId, shiftId, request, override);

        if ("WARNING".equals(response.status())) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

