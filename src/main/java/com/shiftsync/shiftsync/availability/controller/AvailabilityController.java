package com.shiftsync.shiftsync.availability.controller;

import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityItemRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;
import com.shiftsync.shiftsync.availability.service.AvailabilityService;
import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees/me/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Employee availability management endpoints")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final AuthenticationHelper authenticationHelper;

    @PutMapping("/recurring")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "Replace recurring weekly availability",
            description = "Replaces recurring weekly availability windows. Empty list clears all windows."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recurring availability saved", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RecurringAvailabilityResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid request or overlapping windows", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RecurringAvailabilityResponse>> replaceRecurringAvailability(
            Authentication authentication,
            @Valid @RequestBody List<@Valid RecurringAvailabilityItemRequest> windows
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        List<RecurringAvailabilityResponse> response = availabilityService.replaceRecurringAvailability(actorUserId, windows);
        return ResponseEntity.ok(response);
    }
}

