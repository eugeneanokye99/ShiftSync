package com.shiftsync.shiftsync.shift.service.impl;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.availability.entity.RecurringAvailability;
import com.shiftsync.shiftsync.availability.repository.AvailabilityOverrideRepository;
import com.shiftsync.shiftsync.availability.repository.RecurringAvailabilityRepository;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.common.exception.UnprocessableEntityException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeRequest;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeResponse;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftRepository;
import com.shiftsync.shiftsync.shift.service.ShiftAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftAssignmentServiceImpl implements ShiftAssignmentService {

    private static final String AVAILABILITY_MISMATCH = "AVAILABILITY_MISMATCH";

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ManagerLocationRepository managerLocationRepository;
    private final RecurringAvailabilityRepository recurringAvailabilityRepository;
    private final AvailabilityOverrideRepository availabilityOverrideRepository;

    @Override
    @Transactional
    public AssignEmployeeResponse assignEmployee(Long actorUserId, Long shiftId, AssignEmployeeRequest request, boolean override) {
        Employee manager = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            throw new InvalidStateException("Cannot assign employees to a cancelled shift");
        }

        List<Long> assignedLocations = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());
        if (!assignedLocations.contains(shift.getLocation().getId())) {
            throw new AccessDeniedException("You are not assigned to this location");
        }

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new UnprocessableEntityException("Cannot assign an inactive employee to a shift");
        }

        if (shiftAssignmentRepository.existsByShiftIdAndEmployeeId(shiftId, employee.getId())) {
            throw new InvalidStateException("Employee is already assigned to this shift");
        }

        // TODO (Week 3 - FR-CONFLICT-01): reject if employee already assigned to an overlapping shift -> 409
        // TODO (Week 3 - FR-CONFLICT-02): reject if shift falls within an approved leave period -> 409

        boolean availabilityMismatch = isAvailabilityMismatch(employee.getId(), shift);

        // TODO (Week 3 - FR-CONFLICT-04): add overtime threshold warning to conflicts list
        if (availabilityMismatch && !override) {
            return new AssignEmployeeResponse(
                    "WARNING",
                    List.of(AVAILABILITY_MISMATCH),
                    "Employee availability does not match this shift. Re-submit with override=true to proceed.",
                    null
            );
        }

        User assignedBy = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ShiftAssignment assignment = ShiftAssignment.builder()
                .shift(shift)
                .employee(employee)
                .assignedBy(assignedBy)
                .overrideApplied(availabilityMismatch)
                .overrideReason(availabilityMismatch ? AVAILABILITY_MISMATCH : null)
                .build();

        ShiftAssignment saved = shiftAssignmentRepository.save(assignment);

        return new AssignEmployeeResponse(
                "ASSIGNED",
                List.of(),
                "Employee assigned successfully",
                saved.getId()
        );
    }

    private boolean isAvailabilityMismatch(Long employeeId, Shift shift) {
        boolean hasOverrideOnDate = availabilityOverrideRepository.hasOverlap(
                employeeId,
                shift.getShiftDate(),
                shift.getShiftDate()
        );

        if (hasOverrideOnDate) {
            return true;
        }

        List<RecurringAvailability> dayWindows = recurringAvailabilityRepository
                .findByEmployeeAndDay(employeeId, shift.getShiftDate().getDayOfWeek());

        if (dayWindows.isEmpty()) {
            return true;
        }

        return dayWindows.stream().noneMatch(window ->
                !shift.getStartTime().isBefore(window.getStartTime())
                        && !shift.getEndTime().isAfter(window.getEndTime())
        );
    }
}

