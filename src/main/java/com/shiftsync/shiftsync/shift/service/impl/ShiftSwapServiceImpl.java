package com.shiftsync.shiftsync.shift.service.impl;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.NotificationType;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.config.CacheConfig;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapRequest;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapResponse;
import com.shiftsync.shiftsync.shift.entity.ShiftSwapStatus;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftSwap;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftSwapRepository;
import com.shiftsync.shiftsync.shift.service.ShiftSwapService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftSwapServiceImpl implements ShiftSwapService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftSwapRepository shiftSwapRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationService notificationService;
    private final ManagerLocationRepository managerLocationRepository;

    @Override
    @Transactional
    public ShiftSwapResponse requestSwap(Long actorUserId, ShiftSwapRequest request) {
        Employee requester = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        ShiftAssignment requesterAssignment = shiftAssignmentRepository.findById(request.myShiftAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift assignment not found"));

        if (!requesterAssignment.getEmployee().getId().equals(requester.getId())) {
            throw new BadRequestException("You are not assigned to the referenced shift");
        }

        Employee targetEmployee = employeeRepository.findById(request.targetEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Target employee not found"));

        ShiftAssignment targetAssignment = null;
        if (request.targetShiftAssignmentId() != null) {
            targetAssignment = shiftAssignmentRepository.findById(request.targetShiftAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target shift assignment not found"));
            if (!targetAssignment.getEmployee().getId().equals(targetEmployee.getId())) {
                throw new BadRequestException("Target assignment does not belong to the specified target employee");
            }
        }

        ShiftSwap swap = ShiftSwap.builder()
                .requester(requester)
                .requesterAssignment(requesterAssignment)
                .targetEmployee(targetEmployee)
                .targetAssignment(targetAssignment)
                .reason(request.reason())
                .status(ShiftSwapStatus.PENDING_MANAGER_APPROVAL)
                .build();

        ShiftSwap saved = shiftSwapRepository.save(swap);

        Shift requesterShift = requesterAssignment.getShift();
        String proposalMessage = String.format(
                "%s has requested a shift swap with you for the shift on %s from %s to %s at %s.",
                requester.getUser().getFullName(),
                requesterShift.getShiftDate(),
                requesterShift.getStartTime(),
                requesterShift.getEndTime(),
                requesterShift.getLocation().getName()
        );
        notificationService.notifyUser(
                targetEmployee.getUser().getId(),
                NotificationType.SWAP_OUTCOME,
                proposalMessage,
                "SHIFT_SWAP",
                saved.getId()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.LOCATION_SHIFTS, allEntries = true)
    public void approveSwap(Long actorUserId, Long swapId) {
        User manager = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ShiftSwap swap = shiftSwapRepository.findByIdWithDetails(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("Swap request not found"));

        if (swap.getStatus() != ShiftSwapStatus.PENDING_MANAGER_APPROVAL) {
            throw new InvalidStateException("Swap request is not pending approval");
        }

        ShiftAssignment requesterAssignment = swap.getRequesterAssignment();
        ShiftAssignment targetAssignment = swap.getTargetAssignment();
        Employee requester = swap.getRequester();
        Employee targetEmployee = swap.getTargetEmployee();
        Shift requesterShift = requesterAssignment.getShift();

        List<Long> excludedShiftIds = new ArrayList<>();
        excludedShiftIds.add(requesterShift.getId());
        if (targetAssignment != null) {
            excludedShiftIds.add(targetAssignment.getShift().getId());
        }

        checkConflictsForApproval(targetEmployee, requesterShift, excludedShiftIds);

        if (targetAssignment != null) {
            Shift targetShift = targetAssignment.getShift();
            checkConflictsForApproval(requester, targetShift, excludedShiftIds);
        }

        requesterAssignment.setEmployee(targetEmployee);
        requesterAssignment.setAssignedBy(manager);

        if (targetAssignment != null) {
            targetAssignment.setEmployee(requester);
            targetAssignment.setAssignedBy(manager);
        }

        swap.setStatus(ShiftSwapStatus.APPROVED);
        swap.setReviewedBy(manager);

        String requesterMessage = String.format(
                "Your shift swap request for %s has been approved.",
                requesterShift.getShiftDate()
        );
        notificationService.notifyUser(
                requester.getUser().getId(),
                NotificationType.SWAP_OUTCOME,
                requesterMessage,
                "SHIFT_SWAP",
                swapId
        );

        String targetMessage = String.format(
                "The shift swap proposal for %s has been approved by management.",
                requesterShift.getShiftDate()
        );
        notificationService.notifyUser(
                targetEmployee.getUser().getId(),
                NotificationType.SWAP_OUTCOME,
                targetMessage,
                "SHIFT_SWAP",
                swapId
        );
    }

    @Override
    @Transactional
    public void rejectSwap(Long actorUserId, Long swapId, String managerNote) {
        User manager = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ShiftSwap swap = shiftSwapRepository.findByIdWithDetails(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("Swap request not found"));

        if (swap.getStatus() != ShiftSwapStatus.PENDING_MANAGER_APPROVAL) {
            throw new InvalidStateException("Swap request is not pending approval");
        }

        swap.setStatus(ShiftSwapStatus.REJECTED);
        swap.setReviewedBy(manager);
        swap.setManagerNote(managerNote);

        Shift requesterShift = swap.getRequesterAssignment().getShift();

        String requesterMessage = String.format(
                "Your shift swap request for %s has been rejected.%s",
                requesterShift.getShiftDate(),
                managerNote != null ? " Note: " + managerNote : ""
        );
        notificationService.notifyUser(
                swap.getRequester().getUser().getId(),
                NotificationType.SWAP_OUTCOME,
                requesterMessage,
                "SHIFT_SWAP",
                swapId
        );

        String targetMessage = String.format(
                "The shift swap proposal for %s has been rejected by management.",
                requesterShift.getShiftDate()
        );
        notificationService.notifyUser(
                swap.getTargetEmployee().getUser().getId(),
                NotificationType.SWAP_OUTCOME,
                targetMessage,
                "SHIFT_SWAP",
                swapId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftSwapResponse> getMySwaps(Long actorUserId, ShiftSwapStatus status) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (actor.getRole() == UserRole.HR_ADMIN) {
            return shiftSwapRepository.findAllPending()
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        if (actor.getRole() == UserRole.MANAGER) {
            Employee manager = employeeRepository.findByUserId(actorUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));
            List<Long> locationIds = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());
            return shiftSwapRepository.findPendingByLocationIds(locationIds)
                    .stream().map(this::toResponse).collect(Collectors.toList());
        }

        Employee employee = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return shiftSwapRepository.findByParticipant(employee.getId(), status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void checkConflictsForApproval(Employee employee, Shift newShift, List<Long> excludedShiftIds) {
        boolean doubleBooked = shiftAssignmentRepository.existsConflictExcluding(
                employee.getId(),
                excludedShiftIds,
                newShift.getShiftDate(),
                newShift.getStartTime(),
                newShift.getEndTime()
        );
        if (doubleBooked) {
            throw new InvalidStateException(
                    "Conflict detected: " + employee.getUser().getFullName()
                            + " has an overlapping shift on " + newShift.getShiftDate()
            );
        }

        boolean onLeave = leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(
                employee.getId(),
                newShift.getShiftDate(),
                newShift.getShiftDate(),
                List.of(LeaveStatus.APPROVED)
        );
        if (onLeave) {
            throw new InvalidStateException(
                    "Conflict detected: " + employee.getUser().getFullName()
                            + " has approved leave on " + newShift.getShiftDate()
            );
        }
    }

    private ShiftSwapResponse toResponse(ShiftSwap swap) {
        Shift requesterShift = swap.getRequesterAssignment().getShift();
        Shift targetShift = swap.getTargetAssignment() != null
                ? swap.getTargetAssignment().getShift()
                : null;

        return new ShiftSwapResponse(
                swap.getId(),
                swap.getRequester().getId(),
                swap.getRequester().getUser().getFullName(),
                requesterShift.getShiftDate(),
                requesterShift.getStartTime(),
                requesterShift.getEndTime(),
                swap.getTargetEmployee().getId(),
                swap.getTargetEmployee().getUser().getFullName(),
                targetShift != null ? targetShift.getShiftDate() : null,
                targetShift != null ? targetShift.getStartTime() : null,
                targetShift != null ? targetShift.getEndTime() : null,
                swap.getStatus().name(),
                swap.getReason(),
                swap.getManagerNote(),
                swap.getCreatedAt()
        );
    }
}
