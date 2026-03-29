package com.shiftsync.shiftsync.employee.controller;

import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.exception.UnauthorizedException;
import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.employee.dto.CreateEmployeeRequest;
import com.shiftsync.shiftsync.employee.dto.EmployeePageResponse;
import com.shiftsync.shiftsync.employee.dto.EmployeeResponse;
import com.shiftsync.shiftsync.employee.dto.UpdateMyProfileRequest;
import com.shiftsync.shiftsync.employee.service.EmployeeService;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee profile management endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(
            summary = "Create employee profile",
            description = "Creates an employee profile linked to an existing user account."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Employee profile created successfully",
                    content = @Content(schema = @Schema(implementation = EmployeeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Related resource not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Employee profile already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    @Operation(
            summary = "Get paginated employees",
            description = "Returns employee profiles in pages with optional sorting."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Employees fetched successfully",
                    content = @Content(schema = @Schema(implementation = EmployeePageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<EmployeePageResponse> getEmployees(
            Authentication authentication,
            @RequestParam(required = false) Long department,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastName") String sortBy
    ) {
        Long actorUserId = getCurrentUserId(authentication);
        boolean isManager = hasRole(authentication.getAuthorities());

        EmployeePageResponse response = employeeService.getEmployees(
                actorUserId,
                isManager,
                department,
                locationId,
                employmentType,
                active,
                page,
                size,
                sortBy
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get my employee profile", description = "Returns the authenticated employee profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile fetched", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EmployeeResponse> getMyProfile(Authentication authentication) {
        Long actorUserId = getCurrentUserId(authentication);
        return ResponseEntity.ok(employeeService.getMyProfile(actorUserId));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Update my employee profile", description = "Updates phone, skills, and notification preference for the authenticated employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EmployeeResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateMyProfileRequest request
    ) {
        Long actorUserId = getCurrentUserId(authentication);
        return ResponseEntity.ok(employeeService.updateMyProfile(actorUserId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN','MANAGER')")
    @Operation(summary = "Get employee profile by ID", description = "Returns a full employee profile for HR Admins and location-scoped managers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile fetched", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EmployeeResponse> getEmployeeById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long actorUserId = getCurrentUserId(authentication);
        boolean isManager = hasRole(authentication.getAuthorities());
        return ResponseEntity.ok(employeeService.getEmployeeById(actorUserId, isManager, id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('HR_ADMIN')")
    @Operation(summary = "Deactivate employee", description = "Soft-deactivates an employee profile while preserving historical records.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Employee deactivated", content = @Content(schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Employee not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Employee already inactive", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<EmployeeResponse> deactivateEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deactivateEmployee(id));
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long value) {
            return value;
        }
        if (principal instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException("Invalid authentication principal");
            }
        }
        if (principal instanceof UserDetails userDetails) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException("Invalid authentication principal");
            }
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()));
    }
}

