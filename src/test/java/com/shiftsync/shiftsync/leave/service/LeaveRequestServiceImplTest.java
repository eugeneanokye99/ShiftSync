package com.shiftsync.shiftsync.leave.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.availability.entity.AvailabilityOverride;
import com.shiftsync.shiftsync.availability.repository.AvailabilityOverrideRepository;
import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.LeaveType;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.leave.dto.ApproveLeaveRequest;
import com.shiftsync.shiftsync.department.entity.Department;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.leave.dto.CreateLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.GetPendingLeaveRequestsRequest;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestPageResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestResponse;
import com.shiftsync.shiftsync.leave.dto.RejectLeaveRequest;
import com.shiftsync.shiftsync.leave.entity.LeaveRequest;
import com.shiftsync.shiftsync.leave.mapper.LeaveRequestMapper;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.leave.service.impl.LeaveRequestServiceImpl;
import com.shiftsync.shiftsync.location.entity.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private AvailabilityOverrideRepository availabilityOverrideRepository;

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @InjectMocks
    private LeaveRequestServiceImpl leaveRequestService;

    private Employee employee;
    private CreateLeaveRequest request;
    private LeaveRequest leaveRequest;
    private LeaveRequestResponse response;
    private User hrAdmin;

    @BeforeEach
    void setUp() {
        Department department = Department.builder().id(2L).name("Kitchen").build();
        Location location = Location.builder().id(3L).name("Airport Branch").address("Accra").maxHeadcountPerShift(10).active(true).build();

        User user = User.builder()
                .id(5L)
                .email("employee@shiftsync.com")
                .fullName("Employee One")
                .passwordHash("hash")
                .role(UserRole.EMPLOYEE)
                .build();

        hrAdmin = User.builder()
                .id(1L)
                .email("hr@shiftsync.com")
                .fullName("HR Admin")
                .passwordHash("hash")
                .role(UserRole.HR_ADMIN)
                .build();

        employee = Employee.builder()
                .id(20L)
                .user(user)
                .employmentType(com.shiftsync.shiftsync.common.enums.EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .notificationEnabled(true)
                .department(department)
                .location(location)
                .build();

        request = new CreateLeaveRequest(
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                LeaveType.ANNUAL,
                "Family event"
        );

        leaveRequest = LeaveRequest.builder()
                .id(100L)
                .employee(employee)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .leaveType(request.leaveType())
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        response = new LeaveRequestResponse(
                100L,
                20L,
                request.startDate(),
                request.endDate(),
                LeaveType.ANNUAL,
                "Family event",
                LeaveStatus.PENDING,
                LocalDateTime.now()
        );
    }

    @Test
    void createLeaveRequest_Success() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(eq(20L), eq(request.startDate()), eq(request.endDate()), any()))
                .thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);
        when(leaveRequestMapper.toResponse(leaveRequest)).thenReturn(response);

        LeaveRequestResponse created = leaveRequestService.createLeaveRequest(5L, request);

        assertThat(created.id()).isEqualTo(100L);
        assertThat(created.status()).isEqualTo(LeaveStatus.PENDING);
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void createLeaveRequest_OverlappingDates_ThrowsConflict() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(eq(20L), eq(request.startDate()), eq(request.endDate()), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(5L, request))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Leave request overlaps with an existing pending or approved leave request");

        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    @Test
    void createLeaveRequest_EmployeeNotFound_ThrowsNotFound() {
        when(employeeRepository.findByUserId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(5L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Employee profile not found");
    }

    @Test
    void createLeaveRequest_EndDateBeforeStartDate_ThrowsBadRequest() {
        CreateLeaveRequest invalidRequest = new CreateLeaveRequest(
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(1),
                LeaveType.ANNUAL,
                "Family event"
        );

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(5L, invalidRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("End date must be on or after start date");
    }

    @Test
    void getPendingLeaveRequests_ReturnsPagedResponse() {
        GetPendingLeaveRequestsRequest getRequest = new GetPendingLeaveRequestsRequest(
                20L,
                3L,
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 1, 31),
                0,
                10
        );

        PendingLeaveRequestResponse pendingResponse = new PendingLeaveRequestResponse(
                100L,
                20L,
                "Employee One",
                "Kitchen",
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 1, 3),
                LeaveType.ANNUAL,
                3,
                "Family event",
                LeaveStatus.PENDING,
                LocalDateTime.of(2098, 12, 1, 10, 0)
        );

        Page<LeaveRequest> page = new PageImpl<>(List.of(leaveRequest), PageRequest.of(0, 10), 1);

        when(leaveRequestRepository.findAll(org.mockito.ArgumentMatchers.<Specification<LeaveRequest>>any(), any(Pageable.class))).thenReturn(page);
        when(leaveRequestMapper.toPendingResponse(leaveRequest)).thenReturn(pendingResponse);

        PendingLeaveRequestPageResponse result = leaveRequestService.getPendingLeaveRequests(getRequest);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.currentPage()).isEqualTo(0);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().employeeName()).isEqualTo("Employee One");
    }

    @Test
    void approveLeaveRequest_Success() {
        ApproveLeaveRequest approveRequest = new ApproveLeaveRequest("Approved for annual leave");

        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);
        when(leaveRequestMapper.toResponse(leaveRequest)).thenReturn(response);

        LeaveRequestResponse approved = leaveRequestService.approveLeaveRequest(1L, 100L, approveRequest);

        assertThat(approved.id()).isEqualTo(100L);
        verify(availabilityOverrideRepository).save(any(AvailabilityOverride.class));
    }

    @Test
    void approveLeaveRequest_NotPending_ThrowsConflict() {
        ApproveLeaveRequest approveRequest = new ApproveLeaveRequest("Approved for annual leave");
        leaveRequest.setStatus(LeaveStatus.APPROVED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestService.approveLeaveRequest(1L, 100L, approveRequest))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Only pending leave requests can be approved");

        verify(availabilityOverrideRepository, never()).save(any(AvailabilityOverride.class));
    }

    @Test
    void approveLeaveRequest_LeaveNotFound_ThrowsNotFound() {
        ApproveLeaveRequest approveRequest = new ApproveLeaveRequest("Approved for annual leave");

        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveRequestService.approveLeaveRequest(1L, 100L, approveRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Leave request not found");
    }

    @Test
    void rejectLeaveRequest_Success() {
        RejectLeaveRequest rejectRequest = new RejectLeaveRequest("Insufficient staffing");

        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(leaveRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(leaveRequest);
        when(leaveRequestMapper.toResponse(leaveRequest)).thenReturn(response);

        LeaveRequestResponse rejected = leaveRequestService.rejectLeaveRequest(1L, 100L, rejectRequest);

        assertThat(rejected.id()).isEqualTo(100L);
        verify(availabilityOverrideRepository, never()).save(any(AvailabilityOverride.class));
    }

    @Test
    void rejectLeaveRequest_NotPending_ThrowsConflict() {
        RejectLeaveRequest rejectRequest = new RejectLeaveRequest("Insufficient staffing");
        leaveRequest.setStatus(LeaveStatus.APPROVED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(leaveRequest));

        assertThatThrownBy(() -> leaveRequestService.rejectLeaveRequest(1L, 100L, rejectRequest))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Only pending leave requests can be rejected");

        verify(availabilityOverrideRepository, never()).save(any(AvailabilityOverride.class));
    }

    @Test
    void rejectLeaveRequest_LeaveNotFound_ThrowsNotFound() {
        RejectLeaveRequest rejectRequest = new RejectLeaveRequest("Insufficient staffing");

        when(userRepository.findById(1L)).thenReturn(Optional.of(hrAdmin));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveRequestService.rejectLeaveRequest(1L, 100L, rejectRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Leave request not found");
    }
}

