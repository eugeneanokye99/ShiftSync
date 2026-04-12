package com.shiftsync.shiftsync.leave.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.LeaveType;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.leave.dto.CreateLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.entity.LeaveRequest;
import com.shiftsync.shiftsync.leave.mapper.LeaveRequestMapper;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.leave.service.impl.LeaveRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @InjectMocks
    private LeaveRequestServiceImpl leaveRequestService;

    private Employee employee;
    private CreateLeaveRequest request;
    private LeaveRequest leaveRequest;
    private LeaveRequestResponse response;

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
                .id(20L)
                .user(user)
                .employmentType(com.shiftsync.shiftsync.common.enums.EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .notificationEnabled(true)
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
}

