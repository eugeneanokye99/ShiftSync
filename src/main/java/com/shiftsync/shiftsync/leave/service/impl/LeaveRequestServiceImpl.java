package com.shiftsync.shiftsync.leave.service.impl;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.availability.entity.AvailabilityOverride;
import com.shiftsync.shiftsync.availability.repository.AvailabilityOverrideRepository;
import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.leave.dto.ApproveLeaveRequest;
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
import com.shiftsync.shiftsync.leave.service.LeaveRequestService;
import com.shiftsync.shiftsync.leave.specification.LeaveRequestSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AvailabilityOverrideRepository availabilityOverrideRepository;
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
    @Transactional
    public LeaveRequestResponse approveLeaveRequest(Long actorUserId, Long leaveRequestId, ApproveLeaveRequest request) {
        User approver = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidStateException("Only pending leave requests can be approved");
        }

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setHrNote(request.hrNote());
        leaveRequest.setApprovedBy(approver);
        leaveRequest.setApprovedAt(LocalDateTime.now());

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        AvailabilityOverride override = AvailabilityOverride.builder()
                .employee(saved.getEmployee())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .reason("Approved leave request " + saved.getId())
                .build();
        availabilityOverrideRepository.save(override);

        return leaveRequestMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(Long actorUserId, Long leaveRequestId, RejectLeaveRequest request) {
        User reviewer = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidStateException("Only pending leave requests can be rejected");
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setHrNote(request.hrNote());
        leaveRequest.setApprovedBy(reviewer);
        leaveRequest.setApprovedAt(LocalDateTime.now());

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

