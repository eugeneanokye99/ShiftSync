package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.availability.entity.RecurringAvailability;
import com.shiftsync.shiftsync.availability.repository.AvailabilityOverrideRepository;
import com.shiftsync.shiftsync.availability.repository.RecurringAvailabilityRepository;
import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.UnprocessableEntityException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeRequest;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeResponse;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftRepository;
import com.shiftsync.shiftsync.shift.service.impl.ShiftAssignmentServiceImpl;
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
import static org.mockito.ArgumentMatchers.any;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftAssignmentServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ManagerLocationRepository managerLocationRepository;

    @Mock
    private RecurringAvailabilityRepository recurringAvailabilityRepository;

    @Mock
    private AvailabilityOverrideRepository availabilityOverrideRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ShiftAssignmentServiceImpl shiftAssignmentService;

    private Employee manager;
    private Employee employee;
    private Shift shift;
    private User managerUser;

    @BeforeEach
    void setUp() {
        managerUser = User.builder()
                .id(5L)
                .email("manager@shiftsync.com")
                .passwordHash("hash")
                .fullName("Manager")
                .role(UserRole.MANAGER)
                .build();

        User employeeUser = User.builder()
                .id(7L)
                .email("employee@shiftsync.com")
                .passwordHash("hash")
                .fullName("Employee")
                .role(UserRole.EMPLOYEE)
                .build();

        manager = Employee.builder()
                .id(10L)
                .user(managerUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .notificationEnabled(true)
                .build();

        employee = Employee.builder()
                .id(20L)
                .user(employeeUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .notificationEnabled(true)
                .build();

        Location location = Location.builder()
                .id(1L)
                .name("Main")
                .address("Accra")
                .maxHeadcountPerShift(10)
                .active(true)
                .build();

        shift = Shift.builder()
                .id(100L)
                .location(location)
                .shiftDate(LocalDate.of(2026, 4, 7))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(13, 0))
                .minimumHeadcount(1)
                .status(ShiftStatus.OPEN)
                .createdBy(managerUser)
                .build();

        ReflectionTestUtils.setField(shiftAssignmentService, "overtimeBufferMultiplier", 1.10);
    }

    @Test
    void assignEmployee_OutsideAvailability_ReturnsWarningAndDoesNotSave() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.existsByShiftIdAndEmployeeId(100L, 20L)).thenReturn(false);
        when(availabilityOverrideRepository.hasOverlap(20L, LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 7))).thenReturn(false);
        when(recurringAvailabilityRepository.findByEmployeeAndDay(20L, DayOfWeek.TUESDAY)).thenReturn(List.of());

        AssignEmployeeResponse response = shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), false);

        assertThat(response.status()).isEqualTo("WARNING");
        assertThat(response.conflicts()).containsExactly("AVAILABILITY_MISMATCH");
        verify(shiftAssignmentRepository, never()).save(any(ShiftAssignment.class));
    }

    @Test
    void assignEmployee_OutsideAvailabilityWithOverride_CreatesAssignment() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.existsByShiftIdAndEmployeeId(100L, 20L)).thenReturn(false);
        when(availabilityOverrideRepository.hasOverlap(20L, LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 7))).thenReturn(true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(managerUser));
        when(shiftAssignmentRepository.save(any(ShiftAssignment.class))).thenAnswer(invocation -> {
            ShiftAssignment saved = invocation.getArgument(0);
            saved.setId(999L);
            return saved;
        });

        AssignEmployeeResponse response = shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), true);

        assertThat(response.status()).isEqualTo("ASSIGNED");
        assertThat(response.assignmentId()).isEqualTo(999L);
    }

    @Test
    void assignEmployee_WithinAvailability_CreatesAssignmentWithoutOverrideFlag() {
        RecurringAvailability recurring = RecurringAvailability.builder()
                .employee(employee)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .build();

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.existsByShiftIdAndEmployeeId(100L, 20L)).thenReturn(false);
        when(availabilityOverrideRepository.hasOverlap(20L, LocalDate.of(2026, 4, 7), LocalDate.of(2026, 4, 7))).thenReturn(false);
        when(recurringAvailabilityRepository.findByEmployeeAndDay(20L, DayOfWeek.TUESDAY)).thenReturn(List.of(recurring));
        when(userRepository.findById(5L)).thenReturn(Optional.of(managerUser));
        when(shiftAssignmentRepository.save(any(ShiftAssignment.class))).thenAnswer(invocation -> {
            ShiftAssignment saved = invocation.getArgument(0);
            saved.setId(111L);
            return saved;
        });

        AssignEmployeeResponse response = shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), false);

        assertThat(response.status()).isEqualTo("ASSIGNED");
        assertThat(response.assignmentId()).isEqualTo(111L);
    }

    @Test
    void assignEmployee_ManagerNotAssignedToLocation_ThrowsForbidden() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(2L));

        assertThatThrownBy(() -> shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), false))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not assigned to this location");
    }

    @Test
    void assignEmployee_CancelledShift_ThrowsConflict() {
        shift.setStatus(ShiftStatus.CANCELLED);
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), false))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Cannot assign employees to a cancelled shift");
    }

    @Test
    void assignEmployee_InactiveEmployee_ThrowsUnprocessableEntity() {
        employee.setActive(false);

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), false))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Cannot assign an inactive employee to a shift");
    }

    @Test
    void assignEmployee_DoubleBooked_ThrowsConflict() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.existsByShiftIdAndEmployeeId(100L, 20L)).thenReturn(false);
        when(shiftAssignmentRepository.existsOverlappingAssignment(
                eq(20L), eq(100L), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), false))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("overlapping shift");
    }

    @Test
    void assignEmployee_OnApprovedLeave_ThrowsConflict() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.existsByShiftIdAndEmployeeId(100L, 20L)).thenReturn(false);
        when(shiftAssignmentRepository.existsOverlappingAssignment(any(), any(), any(), any(), any())).thenReturn(false);
        when(leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(
                eq(20L), any(), any(), eq(List.of(LeaveStatus.APPROVED)))).thenReturn(true);

        assertThatThrownBy(() -> shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), false))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("approved leave");
    }

    @Test
    void assignEmployee_OvertimeRisk_ReturnsWarning() {
        RecurringAvailability recurring = RecurringAvailability.builder()
                .employee(employee)
                .dayOfWeek(java.time.DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .build();

        // 5 × 9h = 45h existing + 4h new shift = 49h > 44h threshold (40h * 1.10)
        ShiftAssignment existing = ShiftAssignment.builder()
                .shift(Shift.builder()
                        .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(18, 0))
                        .build())
                .build();

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.existsByShiftIdAndEmployeeId(100L, 20L)).thenReturn(false);
        when(shiftAssignmentRepository.existsOverlappingAssignment(any(), any(), any(), any(), any())).thenReturn(false);
        when(leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(any(), any(), any(), any())).thenReturn(false);
        when(availabilityOverrideRepository.hasOverlap(any(), any(), any())).thenReturn(false);
        when(recurringAvailabilityRepository.findByEmployeeAndDay(any(), any())).thenReturn(List.of(recurring));
        when(shiftAssignmentRepository.findByEmployeeInWeek(any(), any(), any()))
                .thenReturn(List.of(existing, existing, existing, existing, existing));

        AssignEmployeeResponse response = shiftAssignmentService.assignEmployee(5L, 100L, new AssignEmployeeRequest(20L), false);

        assertThat(response.status()).isEqualTo("WARNING");
        assertThat(response.conflicts()).contains("OVERTIME_RISK");
    }

    @Test
    void removeAssignment_ExistingAssignment_DeletesAndNotifies() {
        ShiftAssignment assignment = ShiftAssignment.builder()
                .id(1L).shift(shift).employee(employee).assignedBy(managerUser)
                .overrideApplied(false).build();

        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(shiftAssignmentRepository.findByShiftIdAndEmployeeId(100L, 20L)).thenReturn(Optional.of(assignment));

        shiftAssignmentService.removeAssignment(5L, 100L, 20L);

        verify(shiftAssignmentRepository).delete(assignment);
        verify(notificationService).notifyUser(any(), any(), any(), any(), any());
    }

    @Test
    void removeAssignment_AssignmentNotFound_ThrowsNotFound() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(manager));
        when(shiftRepository.findById(100L)).thenReturn(Optional.of(shift));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(10L)).thenReturn(List.of(1L));
        when(shiftAssignmentRepository.findByShiftIdAndEmployeeId(100L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftAssignmentService.removeAssignment(5L, 100L, 20L))
                .isInstanceOf(com.shiftsync.shiftsync.common.exception.ResourceNotFoundException.class)
                .hasMessage("Assignment not found");
    }
}

