package com.shiftsync.shiftsync.shift.service.impl;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.NotificationType;
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
import com.shiftsync.shiftsync.shift.dto.ShiftResponse;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftRepository;
import com.shiftsync.shiftsync.shift.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final LocationRepository locationRepository;
    private final DepartmentRepository departmentRepository;
    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ManagerLocationRepository managerLocationRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ShiftResponse createShift(Long actorUserId, CreateShiftRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (actor.getRole() == UserRole.MANAGER) {
            Employee manager = employeeRepository.findByUserId(actorUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));
            List<Long> assignedLocations = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());
            if (!assignedLocations.contains(request.locationId())) {
                throw new AccessDeniedException("You are not assigned to this location");
            }
        }

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        if (!department.getLocation().getId().equals(request.locationId())) {
            throw new BadRequestException("Department does not belong to the specified location");
        }

        Shift shift = Shift.builder()
                .location(location)
                .department(department)
                .shiftDate(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .requiredSkill(request.requiredSkill())
                .minimumHeadcount(request.minimumHeadcount())
                .createdBy(actor)
                .build();

        Shift saved = shiftRepository.save(shift);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelShift(Long actorUserId, Long shiftId) {
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found"));

        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            throw new InvalidStateException("Shift is already cancelled");
        }

        if (actor.getRole() == UserRole.MANAGER) {
            Employee manager = employeeRepository.findByUserId(actorUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));
            List<Long> assignedLocations = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());
            if (!assignedLocations.contains(shift.getLocation().getId())) {
                throw new AccessDeniedException("You are not assigned to this location");
            }
        }

        shift.setStatus(ShiftStatus.CANCELLED);
        shift.setCancelledAt(LocalDateTime.now());
        shiftRepository.save(shift);

        List<ShiftAssignment> assignments = shiftAssignmentRepository.findByShiftId(shiftId);
        String message = String.format(
                "The shift on %s from %s to %s at %s has been cancelled.",
                shift.getShiftDate(), shift.getStartTime(), shift.getEndTime(),
                shift.getLocation().getName()
        );
        for (ShiftAssignment assignment : assignments) {
            notificationService.notifyUser(
                    assignment.getEmployee().getUser().getId(),
                    NotificationType.SHIFT_CANCELLED,
                    message,
                    "SHIFT",
                    shift.getId()
            );
        }
    }

    private ShiftResponse toResponse(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getLocation().getId(),
                shift.getLocation().getName(),
                shift.getDepartment().getId(),
                shift.getDepartment().getName(),
                shift.getShiftDate(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getRequiredSkill(),
                shift.getMinimumHeadcount(),
                shift.getStatus().name(),
                0
        );
    }
}
