package com.shiftsync.shiftsync.location.controller;

import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.location.dto.LocationResponse;
import com.shiftsync.shiftsync.location.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers")
@RequiredArgsConstructor
@Tag(name = "Manager Locations", description = "Manager location assignment endpoints")
public class ManagerLocationController {

    private final LocationService locationService;
    private final AuthenticationHelper authenticationHelper;

    @GetMapping("/me/locations")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Get assigned locations",
            description = "Returns locations assigned to the authenticated manager."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Locations fetched", content = @Content(array = @ArraySchema(schema = @Schema(implementation = LocationResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<LocationResponse>> getMyAssignedLocations(Authentication authentication) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        return ResponseEntity.ok(locationService.getAssignedLocationsForManager(actorUserId));
    }

    @PostMapping("/{managerEmployeeId}/locations/{locationId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(
            summary = "Assign manager to location",
            description = "Assigns a manager employee profile to a location."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Manager assigned to location"),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Manager or location not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state or duplicate assignment", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> assignManagerToLocation(
            @PathVariable Long managerEmployeeId,
            @PathVariable Long locationId
    ) {
        locationService.assignManagerToLocation(managerEmployeeId, locationId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{managerEmployeeId}/locations/{locationId}")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(
            summary = "Unassign manager from location",
            description = "Removes a manager-location assignment."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Manager unassigned from location"),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Manager, location, or assignment not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> unassignManagerFromLocation(
            @PathVariable Long managerEmployeeId,
            @PathVariable Long locationId
    ) {
        locationService.unassignManagerFromLocation(managerEmployeeId, locationId);
        return ResponseEntity.noContent().build();
    }
}

