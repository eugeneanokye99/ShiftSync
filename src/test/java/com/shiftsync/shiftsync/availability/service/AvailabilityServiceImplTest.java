package com.shiftsync.shiftsync.availability.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.availability.dto.AvailabilityOverrideResponse;
import com.shiftsync.shiftsync.availability.dto.CreateAvailabilityOverrideRequest;
import com.shiftsync.shiftsync.availability.dto.ManagerWeeklyAvailabilityResponse;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityItemRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;
import com.shiftsync.shiftsync.availability.entity.AvailabilityOverride;
import com.shiftsync.shiftsync.availability.entity.RecurringAvailability;
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
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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

    @Mock
    private ManagerLocationRepository managerLocationRepository;

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
        when(availabilityOverrideRepository.hasOverlap(10L, request.startDate(), request.endDate())).thenReturn(false);
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
        when(availabilityOverrideRepository.hasOverlap(10L, request.startDate(), request.endDate())).thenReturn(true);

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
        when(availabilityOverrideRepository.findActiveByEmployee(10L, LocalDate.now()))
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

    @Test
    void getLocationWeeklyAvailability_ManagerNotAssigned_ThrowsForbidden() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(2L));

        assertThatThrownBy(() -> availabilityService.getLocationWeeklyAvailability(5L, 1L, LocalDate.of(2026, 4, 8)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not assigned to this location");
    }

    @Test
    void getLocationWeeklyAvailability_MergesRecurringMinusOverrides() {
        User managerUser = User.builder()
                .id(5L)
                .email("manager@shiftsync.com")
                .fullName("Manager One")
                .passwordHash("hash")
                .role(UserRole.MANAGER)
                .build();

        Employee manager = Employee.builder()
                .id(10L)
                .user(managerUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.now())
                .active(true)
                .notificationEnabled(true)
                .build();

        User teamUser = User.builder()
                .id(7L)
                .email("employee2@shiftsync.com")
                .fullName("Employee Two")
                .passwordHash("hash")
                .role(UserRole.EMPLOYEE)
                .build();

        Employee teamMember = Employee.builder()
                .id(20L)
                .user(teamUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.now())
                .active(true)
                .notificationEnabled(true)
                .build();

        RecurringAvailability mondayWindow = RecurringAvailability.builder()
                .employee(teamMember)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(13, 0))
                .build();

        RecurringAvailability tuesdayWindow = RecurringAvailability.builder()
                .employee(teamMember)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(14, 0))
                .build();

        AvailabilityOverride mondayOverride = AvailabilityOverride.builder()
                .employee(teamMember)
                .startDate(LocalDate.of(2026, 4, 6))
                .endDate(LocalDate.of(2026, 4, 6))
                .reason("Personal")
                .build();

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(employeeRepository.findByLocationIdAndActiveTrueWithUser(1L)).thenReturn(List.of(teamMember));
        when(recurringAvailabilityRepository.findByEmployeeIdIn(List.of(20L))).thenReturn(List.of(mondayWindow, tuesdayWindow));
        when(availabilityOverrideRepository.findWeekOverlaps(
                List.of(20L), LocalDate.of(2026, 4, 6), LocalDate.of(2026, 4, 12)))
                .thenReturn(List.of(mondayOverride));

        ManagerWeeklyAvailabilityResponse response = availabilityService.getLocationWeeklyAvailability(5L, 1L, LocalDate.of(2026, 4, 8));

        assertThat(response.weekStart()).isEqualTo(LocalDate.of(2026, 4, 6));
        assertThat(response.weekEnd()).isEqualTo(LocalDate.of(2026, 4, 12));
        assertThat(response.employees()).hasSize(1);

        List<ManagerWeeklyAvailabilityResponse.DailyAvailability> days = response.employees().getFirst().days();
        assertThat(days).hasSize(7);
        assertThat(days.getFirst().overridden()).isTrue();
        assertThat(days.getFirst().windows()).isEmpty();
        assertThat(days.get(1).overridden()).isFalse();
        assertThat(days.get(1).windows()).hasSize(1);
        assertThat(days.get(1).windows().getFirst().startTime()).isEqualTo(LocalTime.of(10, 0));
    }
}

