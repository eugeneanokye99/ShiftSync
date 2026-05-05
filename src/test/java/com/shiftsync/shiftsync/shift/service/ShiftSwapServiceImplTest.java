package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapRequest;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapResponse;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftSwap;
import com.shiftsync.shiftsync.shift.entity.ShiftSwapStatus;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftSwapRepository;
import com.shiftsync.shiftsync.shift.service.impl.ShiftSwapServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftSwapServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock private ShiftSwapRepository shiftSwapRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private NotificationService notificationService;
    @Mock private ManagerLocationRepository managerLocationRepository;

    @InjectMocks
    private ShiftSwapServiceImpl shiftSwapService;

    private User managerUser;
    private Employee requester;
    private Employee targetEmployee;
    private Shift requesterShift;
    private ShiftAssignment requesterAssignment;
    private ShiftAssignment targetAssignment;

    @BeforeEach
    void setUp() {
        User requesterUser = User.builder().id(1L).fullName("Alice").role(UserRole.EMPLOYEE).build();
        User targetUser = User.builder().id(2L).fullName("Bob").role(UserRole.EMPLOYEE).build();
        managerUser = User.builder().id(3L).fullName("Manager").role(UserRole.MANAGER).build();

        Location location = Location.builder().id(10L).name("HQ").address("Accra").maxHeadcountPerShift(10).active(true).build();

        requester = Employee.builder().id(100L).user(requesterUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2025, 1, 1))
                .active(true).notificationEnabled(true).build();

        targetEmployee = Employee.builder().id(200L).user(targetUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2025, 1, 1))
                .active(true).notificationEnabled(true).build();

        requesterShift = Shift.builder()
                .id(50L).location(location)
                .shiftDate(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0))
                .minimumHeadcount(2).status(ShiftStatus.OPEN).createdBy(managerUser).build();

        Shift targetShift = Shift.builder()
                .id(51L).location(location)
                .shiftDate(LocalDate.of(2026, 6, 2))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0))
                .minimumHeadcount(2).status(ShiftStatus.OPEN).createdBy(managerUser).build();

        requesterAssignment = ShiftAssignment.builder()
                .id(1L).shift(requesterShift).employee(requester).assignedBy(managerUser)
                .overrideApplied(false).build();

        targetAssignment = ShiftAssignment.builder()
                .id(2L).shift(targetShift).employee(targetEmployee).assignedBy(managerUser)
                .overrideApplied(false).build();
    }

    @Test
    void requestSwap_TwoWaySwap_ReturnsPendingResponse() {
        ShiftSwapRequest request = new ShiftSwapRequest(1L, 200L, 2L, "Personal conflict");

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(requester));
        when(shiftAssignmentRepository.findById(1L)).thenReturn(Optional.of(requesterAssignment));
        when(employeeRepository.findById(200L)).thenReturn(Optional.of(targetEmployee));
        when(shiftAssignmentRepository.findById(2L)).thenReturn(Optional.of(targetAssignment));

        ShiftSwap savedSwap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(targetAssignment)
                .reason("Personal conflict").status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL)
                .build();
        when(shiftSwapRepository.save(any(ShiftSwap.class))).thenReturn(savedSwap);

        ShiftSwapResponse response = shiftSwapService.requestSwap(1L, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo("PENDING_MANAGER_APPROVAL");
        assertThat(response.requesterName()).isEqualTo("Alice");
        assertThat(response.targetEmployeeName()).isEqualTo("Bob");
        verify(notificationService).notifyUser(eq(2L), any(), any(), any(), any());
    }

    @Test
    void requestSwap_CoverRequest_NoTargetAssignment_ReturnsPending() {
        ShiftSwapRequest request = new ShiftSwapRequest(1L, 200L, null, "Need cover");

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(requester));
        when(shiftAssignmentRepository.findById(1L)).thenReturn(Optional.of(requesterAssignment));
        when(employeeRepository.findById(200L)).thenReturn(Optional.of(targetEmployee));

        ShiftSwap savedSwap = ShiftSwap.builder()
                .id(11L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(null)
                .reason("Need cover").status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL)
                .build();
        when(shiftSwapRepository.save(any(ShiftSwap.class))).thenReturn(savedSwap);

        ShiftSwapResponse response = shiftSwapService.requestSwap(1L, request);

        assertThat(response.status()).isEqualTo("PENDING_MANAGER_APPROVAL");
        assertThat(response.targetShiftDate()).isNull();
    }

    @Test
    void requestSwap_RequesterNotAssignedToShift_ThrowsBadRequest() {
        ShiftSwapRequest request = new ShiftSwapRequest(1L, 200L, null, null);

        ShiftAssignment otherAssignment = ShiftAssignment.builder()
                .id(1L).shift(requesterShift).employee(targetEmployee)
                .assignedBy(managerUser).overrideApplied(false).build();

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(requester));
        when(shiftAssignmentRepository.findById(1L)).thenReturn(Optional.of(otherAssignment));

        assertThatThrownBy(() -> shiftSwapService.requestSwap(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("You are not assigned to the referenced shift");

        verify(shiftSwapRepository, never()).save(any());
    }

    @Test
    void requestSwap_AssignmentNotFound_ThrowsNotFound() {
        ShiftSwapRequest request = new ShiftSwapRequest(99L, 200L, null, null);

        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(requester));
        when(shiftAssignmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftSwapService.requestSwap(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveSwap_TwoWaySwap_NoConflicts_ReassignsBothAndNotifies() {
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(targetAssignment)
                .status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));
        when(shiftSwapRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(swap));
        when(shiftAssignmentRepository.existsConflictExcluding(any(), any(), any(), any(), any())).thenReturn(false);
        when(leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(any(), any(), any(), any())).thenReturn(false);

        shiftSwapService.approveSwap(3L, 10L);

        assertThat(swap.getStatus()).isEqualTo(ShiftSwapStatus.APPROVED);
        assertThat(swap.getReviewedBy()).isEqualTo(managerUser);
        assertThat(requesterAssignment.getEmployee()).isEqualTo(targetEmployee);
        assertThat(targetAssignment.getEmployee()).isEqualTo(requester);
        verify(notificationService, times(2)).notifyUser(any(), any(), any(), any(), any());
    }

    @Test
    void approveSwap_ConflictDetected_ThrowsInvalidState() {
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(targetAssignment)
                .status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));
        when(shiftSwapRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(swap));
        when(shiftAssignmentRepository.existsConflictExcluding(any(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> shiftSwapService.approveSwap(3L, 10L))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Conflict detected");

        assertThat(swap.getStatus()).isEqualTo(ShiftSwapStatus.PENDING_MANAGER_APPROVAL);
    }

    @Test
    void approveSwap_NotPending_ThrowsInvalidState() {
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(null)
                .status(ShiftSwapStatus.APPROVED).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));
        when(shiftSwapRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(swap));

        assertThatThrownBy(() -> shiftSwapService.approveSwap(3L, 10L))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Swap request is not pending approval");
    }

    @Test
    void approveSwap_LeaveConflictOnApproval_ThrowsInvalidState() {
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(null)
                .status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));
        when(shiftSwapRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(swap));
        when(shiftAssignmentRepository.existsConflictExcluding(any(), any(), any(), any(), any())).thenReturn(false);
        when(leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(
                eq(200L), any(), any(), eq(List.of(LeaveStatus.APPROVED)))).thenReturn(true);

        assertThatThrownBy(() -> shiftSwapService.approveSwap(3L, 10L))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("approved leave");
    }

    @Test
    void rejectSwap_ValidRequest_SetsRejectedAndNotifiesBoth() {
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(null)
                .status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));
        when(shiftSwapRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(swap));

        shiftSwapService.rejectSwap(3L, 10L, "Insufficient staff coverage");

        assertThat(swap.getStatus()).isEqualTo(ShiftSwapStatus.REJECTED);
        assertThat(swap.getManagerNote()).isEqualTo("Insufficient staff coverage");
        assertThat(swap.getReviewedBy()).isEqualTo(managerUser);
        verify(notificationService, times(2)).notifyUser(any(), any(), any(), any(), any());
    }

    @Test
    void rejectSwap_NotPending_ThrowsInvalidState() {
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(null)
                .status(ShiftSwapStatus.REJECTED).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));
        when(shiftSwapRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(swap));

        assertThatThrownBy(() -> shiftSwapService.rejectSwap(3L, 10L, null))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Swap request is not pending approval");
    }

    @Test
    void getMySwaps_Employee_NoStatusFilter_ReturnsAllParticipantSwaps() {
        User employeeUser = requester.getUser();
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(targetAssignment)
                .status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(requester));
        when(shiftSwapRepository.findByParticipant(100L, null)).thenReturn(List.of(swap));

        List<ShiftSwapResponse> result = shiftSwapService.getMySwaps(1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).requesterName()).isEqualTo("Alice");
        assertThat(result.get(0).status()).isEqualTo("PENDING_MANAGER_APPROVAL");
    }

    @Test
    void getMySwaps_Employee_WithStatusFilter_ReturnsFilteredSwaps() {
        User employeeUser = requester.getUser();
        ShiftSwap approved = ShiftSwap.builder()
                .id(11L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(targetAssignment)
                .status(ShiftSwapStatus.APPROVED).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(employeeUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(requester));
        when(shiftSwapRepository.findByParticipant(100L, ShiftSwapStatus.APPROVED)).thenReturn(List.of(approved));

        List<ShiftSwapResponse> result = shiftSwapService.getMySwaps(1L, ShiftSwapStatus.APPROVED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("APPROVED");
    }

    @Test
    void getMySwaps_Manager_ReturnsPendingSwapsForLocation() {
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(targetAssignment)
                .status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL).build();

        Employee managerEmployee = Employee.builder()
                .id(300L).user(managerUser)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2025, 1, 1))
                .active(true).notificationEnabled(true).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));
        when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(managerEmployee));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(300L)).thenReturn(List.of(10L));
        when(shiftSwapRepository.findPendingByLocationIds(List.of(10L))).thenReturn(List.of(swap));

        List<ShiftSwapResponse> result = shiftSwapService.getMySwaps(3L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("PENDING_MANAGER_APPROVAL");
    }

    @Test
    void getMySwaps_HrAdmin_ReturnsAllPendingSwaps() {
        User hrAdminUser = User.builder().id(4L).fullName("HR Admin").role(UserRole.HR_ADMIN).build();
        ShiftSwap swap = ShiftSwap.builder()
                .id(10L).requester(requester).requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee).targetAssignment(targetAssignment)
                .status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL).build();

        when(userRepository.findById(4L)).thenReturn(Optional.of(hrAdminUser));
        when(shiftSwapRepository.findAllPending()).thenReturn(List.of(swap));

        List<ShiftSwapResponse> result = shiftSwapService.getMySwaps(4L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
    }
}
