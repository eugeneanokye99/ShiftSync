package com.shiftsync.shiftsync.leave.service.impl;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.leave.dto.CreateLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.GetPendingLeaveRequestsRequest;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestPageResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestResponse;
import com.shiftsync.shiftsync.leave.entity.LeaveRequest;
import com.shiftsync.shiftsync.leave.mapper.LeaveRequestMapper;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.leave.service.LeaveRequestService;
import com.shiftsync.shiftsync.leave.specification.LeaveRequestSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveRequestMapper leaveRequestMapper;

    @Override
    @Transactional
    public LeaveRequestResponse createLeaveRequest(Long actorUserId, CreateLeaveRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        Employee employee = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        boolean overlapExists = leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(
                employee.getId(),
                request.startDate(),
                request.endDate(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)
        );

        if (overlapExists) {
            throw new InvalidStateException("Leave request overlaps with an existing pending or approved leave request");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .leaveType(request.leaveType())
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return leaveRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PendingLeaveRequestPageResponse getPendingLeaveRequests(GetPendingLeaveRequestsRequest request) {
        Pageable pageable = PageRequest.of(
                request.page(),
                request.size(),
                Sort.by(Sort.Direction.ASC, "submittedAt")
        );

        Page<LeaveRequest> page = leaveRequestRepository.findAll(
                LeaveRequestSpecification.withPendingFilters(
                        request.employeeId(),
                        request.locationId(),
                        request.startDate(),
                        request.endDate()
                ),
                pageable
        );

        List<PendingLeaveRequestResponse> content = page.getContent().stream()
                .map(leaveRequestMapper::toPendingResponse)
                .toList();

        return new PendingLeaveRequestPageResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber()
        );
    }
}

