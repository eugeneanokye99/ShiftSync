package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.department.entity.Department;
import com.shiftsync.shiftsync.department.repository.DepartmentRepository;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.repository.LocationRepository;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import com.shiftsync.shiftsync.shift.dto.CreateShiftRequest;
import com.shiftsync.shiftsync.shift.dto.EmployeeShiftResponse;
import com.shiftsync.shiftsync.shift.dto.LocationShiftPageResponse;
import com.shiftsync.shiftsync.shift.dto.ShiftResponse;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.entity.StaffingStatus;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftRepository;
import com.shiftsync.shiftsync.shift.service.impl.ShiftServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock private ManagerLocationRepository managerLocationRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ShiftServiceImpl shiftService;

    private User managerUser;
    private User hrAdminUser;
    private Employee manager;
    private Employee employee;
    private Location location;
    private Department department;
    private Shift shift;

    @BeforeEach
    void setUp() {
        managerUser = User.builder().id(1L).fullName("Manager").role(UserRole.MANAGER).build();
        hrAdminUser = User.builder().id(2L).fullName("HR Admin").role(UserRole.HR_ADMIN).build();

        location = Location.builder().id(10L).name("HQ").address("Accra").maxHeadcountPerShift(10).active(true).build();
        department = Department.builder().id(20L).name("Engineering").location(location).build();

        manager = Employee.builder()
                .id(100L).user(managerUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2025, 1, 1))
                .active(true).notificationEnabled(true)
                .build();

        User employeeUser = User.builder().id(3L).fullName("John Doe").role(UserRole.EMPLOYEE).build();
        employee = Employee.builder()
                .id(200L).user(employeeUser)
                .employmentType(EmploymentType.PART_TIME)
                .contractedWeeklyHours(new BigDecimal("20.00"))
                .hireDate(LocalDate.of(2025, 6, 1))
                .active(true).notificationEnabled(true)
                .build();

        shift = Shift.builder()
                .id(50L).location(location).department(department)
                .shiftDate(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0))
                .minimumHeadcount(2).status(ShiftStatus.OPEN).createdBy(managerUser)
                .build();
    }

    @Test
    void createShift_ValidManagerRequest_ReturnsShiftResponse() {
        CreateShiftRequest request = new CreateShiftRequest(
                10L, 20L, LocalDate.of(2026, 6, 1),
                LocalTime.of(9, 0), LocalTime.of(17, 0), null, 2
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(managerUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(100L)).thenReturn(List.of(10L));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(departmentRepository.findById(20L)).thenReturn(Optional.of(department));
        when(shiftRepository.save(any(Shift.class))).thenReturn(shift);

        ShiftResponse response = shiftService.createShift(1L, request);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.assignedCount()).isEqualTo(0);
        verify(shiftRepository).save(any(Shift.class));
    }

    @Test
    void createShift_EndTimeBeforeStartTime_ThrowsBadRequest() {
        CreateShiftRequest request = new CreateShiftRequest(
                10L, 20L, LocalDate.of(2026, 6, 1),
                LocalTime.of(17, 0), LocalTime.of(9, 0), null, 1
        );

        assertThatThrownBy(() -> shiftService.createShift(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("End time must be after start time");

        verify(shiftRepository, never()).save(any());
    }

    @Test
    void createShift_ManagerNotAssignedToLocation_ThrowsAccessDenied() {
        CreateShiftRequest request = new CreateShiftRequest(
                10L, 20L, LocalDate.of(2026, 6, 1),
                LocalTime.of(9, 0), LocalTime.of(17, 0), null, 1
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(managerUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(100L)).thenReturn(List.of(99L));

        assertThatThrownBy(() -> shiftService.createShift(1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createShift_HrAdminBypassesLocationCheck_CreatesShift() {
        CreateShiftRequest request = new CreateShiftRequest(
                10L, 20L, LocalDate.of(2026, 6, 1),
                LocalTime.of(9, 0), LocalTime.of(17, 0), null, 1
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(hrAdminUser));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(departmentRepository.findById(20L)).thenReturn(Optional.of(department));
        when(shiftRepository.save(any(Shift.class))).thenReturn(shift);

        ShiftResponse response = shiftService.createShift(2L, request);

        assertThat(response).isNotNull();
        verify(employeeRepository, never()).findByUserId(2L);
    }

    @Test
    void createShift_DepartmentNotInLocation_ThrowsBadRequest() {
        Location otherLocation = Location.builder().id(99L).name("Other").address("X").maxHeadcountPerShift(5).active(true).build();
        Department wrongDept = Department.builder().id(20L).name("Sales").location(otherLocation).build();

        CreateShiftRequest request = new CreateShiftRequest(
                10L, 20L, LocalDate.of(2026, 6, 1),
                LocalTime.of(9, 0), LocalTime.of(17, 0), null, 1
        );

        when(userRepository.findById(2L)).thenReturn(Optional.of(hrAdminUser));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(departmentRepository.findById(20L)).thenReturn(Optional.of(wrongDept));

        assertThatThrownBy(() -> shiftService.createShift(2L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Department does not belong to the specified location");
    }

    @Test
    void cancelShift_OpenShift_CancelsAndNotifiesAssignees() {
        ShiftAssignment assignment = ShiftAssignment.builder()
                .id(1L).shift(shift).employee(employee).assignedBy(managerUser)
                .overrideApplied(false).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(managerUser));
        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(100L)).thenReturn(List.of(10L));
        when(shiftAssignmentRepository.findByShiftId(50L)).thenReturn(List.of(assignment));

        shiftService.cancelShift(1L, 50L);

        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.CANCELLED);
        assertThat(shift.getCancelledAt()).isNotNull();
        verify(notificationService).notifyUser(any(), any(), any(), any(), any());
    }

    @Test
    void cancelShift_AlreadyCancelled_ThrowsConflict() {
        shift.setStatus(ShiftStatus.CANCELLED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(managerUser));
        when(shiftRepository.findById(50L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> shiftService.cancelShift(1L, 50L))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Shift is already cancelled");
    }

    @Test
    void cancelShift_ShiftNotFound_ThrowsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(managerUser));
        when(shiftRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftService.cancelShift(1L, 50L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyShifts_EmployeeHasShifts_ReturnsResponsesWithColleagues() {
        ShiftAssignment myAssignment = ShiftAssignment.builder()
                .id(1L).shift(shift).employee(employee).assignedBy(managerUser)
                .overrideApplied(false).build();

        User colleagueUser = User.builder().id(4L).fullName("Jane Smith").role(UserRole.EMPLOYEE).build();
        Employee colleague = Employee.builder().id(300L).user(colleagueUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40")).hireDate(LocalDate.now())
                .active(true).notificationEnabled(true).build();
        ShiftAssignment colleagueAssignment = ShiftAssignment.builder()
                .id(2L).shift(shift).employee(colleague).assignedBy(managerUser)
                .overrideApplied(false).build();

        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.findByEmployeeInRange(any(), any(), any(), any()))
                .thenReturn(List.of(myAssignment));
        when(shiftAssignmentRepository.findColleaguesByShiftIds(any(), any()))
                .thenReturn(List.of(colleagueAssignment));

        List<EmployeeShiftResponse> result = shiftService.getMyShifts(
                3L, LocalDate.now(), LocalDate.now().plusDays(7), false
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).shiftId()).isEqualTo(50L);
        assertThat(result.get(0).assignedColleagues()).containsExactly("Jane Smith");
    }

    @Test
    void getMyShifts_NoShifts_ReturnsEmptyList() {
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(employee));
        when(shiftAssignmentRepository.findByEmployeeInRange(any(), any(), any(), any()))
                .thenReturn(List.of());

        List<EmployeeShiftResponse> result = shiftService.getMyShifts(
                3L, LocalDate.now(), LocalDate.now().plusDays(7), false
        );

        assertThat(result).isEmpty();
    }

    @Test
    void getLocationShifts_ManagerAssignedToLocation_ReturnsPageWithStaffingStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(managerUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(100L)).thenReturn(List.of(10L));
        when(shiftRepository.findByLocationInRange(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7)))
                .thenReturn(List.of(shift));
        when(shiftAssignmentRepository.findAssignmentsByShiftIds(any())).thenReturn(List.of());

        LocationShiftPageResponse response = shiftService.getLocationShifts(
                1L, 10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), null, 0, 20
        );

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).staffingStatus()).isEqualTo(StaffingStatus.UNDERSTAFFED);
        assertThat(response.content().get(0).assignedCount()).isEqualTo(0);
    }

    @Test
    void getLocationShifts_ManagerNotAssigned_ThrowsAccessDenied() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(managerUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(manager));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(100L)).thenReturn(List.of(99L));

        assertThatThrownBy(() -> shiftService.getLocationShifts(
                1L, 10L, LocalDate.now(), LocalDate.now().plusDays(6), null, 0, 20
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getLocationShifts_WithDepartmentFilter_ReturnsOnlyMatchingShifts() {
        Department otherDept = Department.builder().id(99L).name("HR").location(location).build();
        Shift otherShift = Shift.builder().id(51L).location(location).department(otherDept)
                .shiftDate(LocalDate.of(2026, 6, 2)).startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(16, 0)).minimumHeadcount(1).status(ShiftStatus.OPEN)
                .createdBy(managerUser).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(hrAdminUser));
        when(shiftRepository.findByLocationInRange(10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7)))
                .thenReturn(List.of(shift, otherShift));
        when(shiftAssignmentRepository.findAssignmentsByShiftIds(any())).thenReturn(List.of());

        LocationShiftPageResponse response = shiftService.getLocationShifts(
                2L, 10L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7), 20L, 0, 20
        );

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().get(0).shiftId()).isEqualTo(50L);
    }
}
