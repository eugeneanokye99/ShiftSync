package com.shiftsync.shiftsync.availability.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.availability.dto.AvailabilityOverrideResponse;
import com.shiftsync.shiftsync.availability.dto.CreateAvailabilityOverrideRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityItemRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;
import com.shiftsync.shiftsync.availability.entity.AvailabilityOverride;
import com.shiftsync.shiftsync.availability.repository.AvailabilityOverrideRepository;
import com.shiftsync.shiftsync.availability.repository.RecurringAvailabilityRepository;
import com.shiftsync.shiftsync.availability.service.impl.AvailabilityServiceImpl;
import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RecurringAvailabilityRepository recurringAvailabilityRepository;

    @Mock
    private AvailabilityOverrideRepository availabilityOverrideRepository;

    @InjectMocks
    private AvailabilityServiceImpl availabilityService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(5L)
                .email("employee@shiftsync.com")
                .fullName("Employee One")
                .passwordHash("hash")
                .role(UserRole.EMPLOYEE)
                .build();

        employee = Employee.builder()
                .id(10L)
                .user(user)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.now())
                .active(true)
                .notificationEnabled(true)
                .build();
    }

    @Test
    void replaceRecurringAvailability_SameDayOverlap_ThrowsBadRequest() {
        List<RecurringAvailabilityItemRequest> request = List.of(
                new RecurringAvailabilityItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                new RecurringAvailabilityItemRequest(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(16, 0))
        );

        assertThatThrownBy(() -> availabilityService.replaceRecurringAvailability(5L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Overlapping recurring availability windows are not allowed for MONDAY");

        verify(employeeRepository, never()).findByUserId(5L);
    }

    @Test
    void replaceRecurringAvailability_DifferentDays_AllowsSameTimeRange() {
        List<RecurringAvailabilityItemRequest> request = List.of(
                new RecurringAvailabilityItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                new RecurringAvailabilityItemRequest(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))
        );

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(recurringAvailabilityRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecurringAvailabilityResponse> response = availabilityService.replaceRecurringAvailability(5L, request);

        assertThat(response).hasSize(2);
        verify(recurringAvailabilityRepository).deleteByEmployeeId(10L);
    }

    @Test
    void replaceRecurringAvailability_AdjacentWindows_SameDay_AreAllowed() {
        List<RecurringAvailabilityItemRequest> request = List.of(
                new RecurringAvailabilityItemRequest(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                new RecurringAvailabilityItemRequest(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(16, 0))
        );

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(recurringAvailabilityRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecurringAvailabilityResponse> response = availabilityService.replaceRecurringAvailability(5L, request);

        assertThat(response).hasSize(2);
    }

    @Test
    void replaceRecurringAvailability_EmptyList_ClearsAndReturnsEmpty() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));

        List<RecurringAvailabilityResponse> response = availabilityService.replaceRecurringAvailability(5L, List.of());

        assertThat(response).isEmpty();
        verify(recurringAvailabilityRepository).deleteByEmployeeId(10L);
        verify(recurringAvailabilityRepository, never()).saveAll(anyList());
    }

    @Test
    void replaceRecurringAvailability_EndBeforeStart_ThrowsBadRequest() {
        List<RecurringAvailabilityItemRequest> request = List.of(
                new RecurringAvailabilityItemRequest(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(9, 0))
        );

        assertThatThrownBy(() -> availabilityService.replaceRecurringAvailability(5L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("endTime must be after startTime for MONDAY");
    }

    @Test
    void createOverride_ValidRequest_ReturnsCreatedOverride() {
        CreateAvailabilityOverrideRequest request = new CreateAvailabilityOverrideRequest(
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                "Family event"
        );

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(availabilityOverrideRepository.existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                10L, request.endDate(), request.startDate())).thenReturn(false);
        when(availabilityOverrideRepository.save(any(AvailabilityOverride.class))).thenAnswer(invocation -> {
            AvailabilityOverride override = invocation.getArgument(0);
            override.setId(77L);
            return override;
        });

        AvailabilityOverrideResponse response = availabilityService.createOverride(5L, request);

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 4, 12));
        verify(availabilityOverrideRepository).save(any(AvailabilityOverride.class));
    }

    @Test
    void createOverride_Overlap_ThrowsConflict() {
        CreateAvailabilityOverrideRequest request = new CreateAvailabilityOverrideRequest(
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 12),
                "Travel"
        );

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(availabilityOverrideRepository.existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                10L, request.endDate(), request.startDate())).thenReturn(true);

        assertThatThrownBy(() -> availabilityService.createOverride(5L, request))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Overlapping override dates are not allowed");

        verify(availabilityOverrideRepository, never()).save(any(AvailabilityOverride.class));
    }

    @Test
    void createOverride_EndBeforeStart_ThrowsBadRequest() {
        CreateAvailabilityOverrideRequest request = new CreateAvailabilityOverrideRequest(
                LocalDate.of(2026, 4, 12),
                LocalDate.of(2026, 4, 10),
                null
        );

        assertThatThrownBy(() -> availabilityService.createOverride(5L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("endDate must be on or after startDate");
    }

    @Test
    void listActiveOverrides_ReturnsOnlyRepositoryResults() {
        AvailabilityOverride current = AvailabilityOverride.builder()
                .id(1L)
                .employee(employee)
                .startDate(LocalDate.of(2026, 4, 10))
                .endDate(LocalDate.of(2026, 4, 11))
                .reason("Travel")
                .build();

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(availabilityOverrideRepository.findByEmployeeIdAndEndDateGreaterThanEqualOrderByStartDateAsc(10L, LocalDate.now()))
                .thenReturn(List.of(current));

        List<AvailabilityOverrideResponse> response = availabilityService.listActiveOverrides(5L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(1L);
    }

    @Test
    void deleteOverride_NotOwned_ThrowsNotFound() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(availabilityOverrideRepository.findByIdAndEmployeeId(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.deleteOverride(5L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Availability override not found");
    }
}

