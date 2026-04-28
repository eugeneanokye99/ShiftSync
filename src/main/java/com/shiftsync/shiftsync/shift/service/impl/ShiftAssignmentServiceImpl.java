package com.shiftsync.shiftsync.shift.service.impl;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.availability.entity.RecurringAvailability;
import com.shiftsync.shiftsync.availability.repository.AvailabilityOverrideRepository;
import com.shiftsync.shiftsync.availability.repository.RecurringAvailabilityRepository;
import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.NotificationType;
import com.shiftsync.shiftsync.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.common.exception.UnprocessableEntityException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeRequest;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeResponse;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftRepository;
import com.shiftsync.shiftsync.shift.service.ShiftAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftAssignmentServiceImpl implements ShiftAssignmentService {

    private static final String AVAILABILITY_MISMATCH = "AVAILABILITY_MISMATCH";
    private static final String OVERTIME_RISK = "OVERTIME_RISK";

    @Value("${shiftsync.overtime.buffer-multiplier:1.10}")
    private double overtimeBufferMultiplier;

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ManagerLocationRepository managerLocationRepository;
    private final RecurringAvailabilityRepository recurringAvailabilityRepository;
    private final AvailabilityOverrideRepository availabilityOverrideRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.LOCATION_SHIFTS, allEntries = true)
    public AssignEmployeeResponse assignEmployee(Long actorUserId, Long shiftId, AssignEmployeeRequest request, boolean override) {
        Employee manager = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));

        Shift shift = shiftRepository.findByIdForUpdate(shiftId)
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

        boolean doubleBooked = shiftAssignmentRepository.existsOverlappingAssignment(
                employee.getId(),
                shiftId,
                shift.getShiftDate(),
                shift.getStartTime(),
                shift.getEndTime()
        );
        if (doubleBooked) {
            throw new InvalidStateException("Employee is already assigned to an overlapping shift on this date");
        }

        boolean onApprovedLeave = leaveRequestRepository.existsOverlappingByEmployeeAndStatuses(
                employee.getId(),
                shift.getShiftDate(),
                shift.getShiftDate(),
                List.of(LeaveStatus.APPROVED)
        );
        if (onApprovedLeave) {
            throw new InvalidStateException("Employee has approved leave that overlaps with this shift");
        }

        List<String> conflicts = new ArrayList<>();

        if (isAvailabilityMismatch(employee.getId(), shift)) {
            conflicts.add(AVAILABILITY_MISMATCH);
        }

        if (isOvertimeRisk(employee, shift)) {
            conflicts.add(OVERTIME_RISK);
        }

        if (!conflicts.isEmpty() && !override) {
            return new AssignEmployeeResponse(
                    "WARNING",
                    conflicts,
                    "Conflicts detected. Re-submit with override=true to proceed.",
                    null
            );
        }

        User assignedBy = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ShiftAssignment assignment = ShiftAssignment.builder()
                .shift(shift)
                .employee(employee)
                .assignedBy(assignedBy)
                .overrideApplied(!conflicts.isEmpty())
                .overrideReason(!conflicts.isEmpty() ? String.join(", ", conflicts) : null)
                .build();

        ShiftAssignment saved = shiftAssignmentRepository.save(assignment);

        String message = String.format(
                "You have been assigned to a shift on %s from %s to %s at %s.",
                shift.getShiftDate(), shift.getStartTime(), shift.getEndTime(),
                shift.getLocation().getName()
        );
        notificationService.notifyUser(
                employee.getUser().getId(),
                NotificationType.SHIFT_ASSIGNED,
                message,
                "SHIFT",
                shift.getId()
        );

        return new AssignEmployeeResponse("ASSIGNED", List.of(), "Employee assigned successfully", saved.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.LOCATION_SHIFTS, allEntries = true)
    public void removeAssignment(Long actorUserId, Long shiftId, Long employeeId) {
        Employee manager = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        List<Long> assignedLocations = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());
        if (!assignedLocations.contains(shift.getLocation().getId())) {
            throw new AccessDeniedException("You are not assigned to this location");
        }

        ShiftAssignment assignment = shiftAssignmentRepository.findByShiftIdAndEmployeeId(shiftId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        shiftAssignmentRepository.delete(assignment);

        String message = String.format(
                "You have been removed from the shift on %s from %s to %s at %s.",
                shift.getShiftDate(), shift.getStartTime(), shift.getEndTime(),
                shift.getLocation().getName()
        );
        notificationService.notifyUser(
                assignment.getEmployee().getUser().getId(),
                NotificationType.SHIFT_REMOVED,
                message,
                "SHIFT",
                shift.getId()
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

    private boolean isOvertimeRisk(Employee employee, Shift shift) {
        var weekStart = shift.getShiftDate().with(DayOfWeek.MONDAY);
        var weekEnd = shift.getShiftDate().with(DayOfWeek.SUNDAY);

        List<ShiftAssignment> weekAssignments = shiftAssignmentRepository
                .findByEmployeeInWeek(employee.getId(), weekStart, weekEnd);

        double assignedHours = weekAssignments.stream()
                .mapToDouble(a -> ChronoUnit.MINUTES.between(
                        a.getShift().getStartTime(), a.getShift().getEndTime()) / 60.0)
                .sum();

        double newShiftHours = ChronoUnit.MINUTES.between(
                shift.getStartTime(), shift.getEndTime()) / 60.0;

        double threshold = employee.getContractedWeeklyHours().doubleValue() * overtimeBufferMultiplier;
        return (assignedHours + newShiftHours) > threshold;
    }
}
