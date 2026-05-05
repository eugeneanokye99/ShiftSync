package com.shiftsync.shiftsync.shift.controller;

import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.shift.dto.CreateShiftRequest;
import com.shiftsync.shiftsync.shift.dto.ShiftResponse;
import com.shiftsync.shiftsync.shift.dto.UpdateShiftRequest;
import com.shiftsync.shiftsync.shift.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
@Tag(name = "Shifts", description = "Shift management endpoints")
public class ShiftController {

    private final ShiftService shiftService;
    private final AuthenticationHelper authenticationHelper;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Create a shift",
            description = "Creates a new shift for a location and department. Managers must be assigned to the specified location."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Shift created", content = @Content(schema = @Schema(implementation = ShiftResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or end time not after start time", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Manager not assigned to the location", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Location or department not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ShiftResponse> createShift(
            Authentication authentication,
            @Valid @RequestBody CreateShiftRequest request
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        ShiftResponse response = shiftService.createShift(actorUserId, request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(response.id())
                        .toUri())
                .body(response);
    }

    @PatchMapping("/{shiftId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Update a shift",
            description = "Partially updates an open shift. All fields are optional — only provided fields are applied. Notifies all assigned employees asynchronously. Cannot update a cancelled shift."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shift updated", content = @Content(schema = @Schema(implementation = ShiftResponse.class))),
            @ApiResponse(responseCode = "400", description = "End time not after start time", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Manager not assigned to the location", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Shift not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Shift is cancelled", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ShiftResponse> updateShift(
            Authentication authentication,
            @PathVariable Long shiftId,
            @Valid @RequestBody UpdateShiftRequest request
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        return ResponseEntity.ok(shiftService.updateShift(actorUserId, shiftId, request));
    }

    @PatchMapping("/{shiftId}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR_ADMIN')")
    @Operation(
            summary = "Cancel a shift",
            description = "Cancels a shift and notifies all assigned employees. Managers must be assigned to the shift's location."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Shift cancelled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Manager not assigned to the location", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Shift not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Shift already cancelled", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cancelShift(
            Authentication authentication,
            @PathVariable Long shiftId
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        shiftService.cancelShift(actorUserId, shiftId);
        return ResponseEntity.noContent().build();
    }
}
