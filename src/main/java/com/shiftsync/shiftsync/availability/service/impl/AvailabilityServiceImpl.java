package com.shiftsync.shiftsync.availability.service.impl;

import com.shiftsync.shiftsync.availability.dto.AvailabilityOverrideResponse;
import com.shiftsync.shiftsync.availability.dto.CreateAvailabilityOverrideRequest;
import com.shiftsync.shiftsync.availability.dto.ManagerWeeklyAvailabilityResponse;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityItemRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;
import com.shiftsync.shiftsync.availability.entity.AvailabilityOverride;
import com.shiftsync.shiftsync.availability.entity.RecurringAvailability;
import com.shiftsync.shiftsync.availability.repository.AvailabilityOverrideRepository;
import com.shiftsync.shiftsync.availability.repository.RecurringAvailabilityRepository;
import com.shiftsync.shiftsync.availability.service.AvailabilityService;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final EmployeeRepository employeeRepository;
    private final RecurringAvailabilityRepository recurringAvailabilityRepository;
    private final AvailabilityOverrideRepository availabilityOverrideRepository;
    private final ManagerLocationRepository managerLocationRepository;

    @Override
    @Transactional
    public List<RecurringAvailabilityResponse> replaceRecurringAvailability(
            Long actorUserId,
            List<RecurringAvailabilityItemRequest> windows
    ) {
        if (windows == null) {
            throw new BadRequestException("Recurring availability payload is required");
        }

        validateTimeOrder(windows);
        validateNoSameDayOverlaps(windows);

        Employee employee = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        recurringAvailabilityRepository.deleteByEmployeeId(employee.getId());

        if (windows.isEmpty()) {
            return List.of();
        }

        List<RecurringAvailability> entities = windows.stream()
                .map(item -> RecurringAvailability.builder()
                        .employee(employee)
                        .dayOfWeek(item.dayOfWeek())
                        .startTime(item.startTime())
                        .endTime(item.endTime())
                        .build())
                .toList();

        List<RecurringAvailability> saved = recurringAvailabilityRepository.saveAll(entities);

        return saved.stream()
                .sorted(Comparator
                        .comparing(RecurringAvailability::getDayOfWeek)
                        .thenComparing(RecurringAvailability::getStartTime))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AvailabilityOverrideResponse createOverride(Long actorUserId, CreateAvailabilityOverrideRequest request) {
        validateDateOrder(request.startDate(), request.endDate());

        Employee employee = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        boolean overlaps = availabilityOverrideRepository
                .hasOverlap(employee.getId(), request.startDate(), request.endDate());

        if (overlaps) {
            throw new InvalidStateException("Overlapping override dates are not allowed");
        }

        AvailabilityOverride saved = availabilityOverrideRepository.save(
                AvailabilityOverride.builder()
                        .employee(employee)
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .reason(request.reason())
                        .build()
        );

        return toOverrideResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityOverrideResponse> listActiveOverrides(Long actorUserId) {
        Employee employee = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        return availabilityOverrideRepository
                .findActiveByEmployee(employee.getId(), LocalDate.now())
                .stream()
                .map(this::toOverrideResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteOverride(Long actorUserId, Long overrideId) {
        Employee employee = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        AvailabilityOverride override = availabilityOverrideRepository.findByIdAndEmployeeId(overrideId, employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability override not found"));

        availabilityOverrideRepository.delete(override);
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerWeeklyAvailabilityResponse getLocationWeeklyAvailability(Long actorUserId, Long locationId, LocalDate weekDate) {
        if (weekDate == null) {
            throw new BadRequestException("week is required");
        }

        Employee manager = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));

        List<Long> assignedLocationIds = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());
        if (!assignedLocationIds.contains(locationId)) {
            throw new AccessDeniedException("You are not assigned to this location");
        }

        LocalDate weekStart = weekDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Employee> employees = employeeRepository.findByLocationIdAndActiveTrueWithUser(locationId);
        if (employees.isEmpty()) {
            return new ManagerWeeklyAvailabilityResponse(weekStart, weekEnd, List.of());
        }

        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();

        Map<EmployeeDayKey, List<ManagerWeeklyAvailabilityResponse.TimeWindow>> recurringByEmployeeDay =
                recurringAvailabilityRepository.findByEmployeeIdIn(employeeIds).stream()
                        .collect(Collectors.groupingBy(
                                recurring -> new EmployeeDayKey(recurring.getEmployee().getId(), recurring.getDayOfWeek()),
                                Collectors.mapping(
                                        recurring -> new ManagerWeeklyAvailabilityResponse.TimeWindow(
                                                recurring.getStartTime(),
                                                recurring.getEndTime()
                                        ),
                                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                                .sorted(Comparator.comparing(ManagerWeeklyAvailabilityResponse.TimeWindow::startTime))
                                                .toList())
                                )
                        ));

        Map<Long, Set<LocalDate>> blockedDatesByEmployee = availabilityOverrideRepository
                .findWeekOverlaps(employeeIds, weekStart, weekEnd)
                .stream()
                .collect(Collectors.groupingBy(
                        override -> override.getEmployee().getId(),
                        Collectors.flatMapping(
                                override -> override.getStartDate().datesUntil(override.getEndDate().plusDays(1)),
                                Collectors.toSet()
                        )
                ));

        List<ManagerWeeklyAvailabilityResponse.EmployeeWeeklyAvailability> employeeAvailability = employees.stream()
                .map(employee -> {
                    List<ManagerWeeklyAvailabilityResponse.DailyAvailability> dailyAvailability = new ArrayList<>();

                    for (int i = 0; i < 7; i++) {
                        LocalDate date = weekStart.plusDays(i);
                        DayOfWeek day = date.getDayOfWeek();
                        boolean overridden = blockedDatesByEmployee
                                .getOrDefault(employee.getId(), Set.of())
                                .contains(date);

                        List<ManagerWeeklyAvailabilityResponse.TimeWindow> windows = overridden
                                ? List.of()
                                : recurringByEmployeeDay.getOrDefault(new EmployeeDayKey(employee.getId(), day), List.of());

                        dailyAvailability.add(new ManagerWeeklyAvailabilityResponse.DailyAvailability(
                                date,
                                day,
                                overridden,
                                windows
                        ));
                    }

                    return new ManagerWeeklyAvailabilityResponse.EmployeeWeeklyAvailability(
                            employee.getId(),
                            employee.getUser().getFullName(),
                            dailyAvailability
                    );
                })
                .toList();

        return new ManagerWeeklyAvailabilityResponse(weekStart, weekEnd, employeeAvailability);
    }

    private void validateTimeOrder(List<RecurringAvailabilityItemRequest> windows) {
        windows.forEach(item -> {
            if (!item.endTime().isAfter(item.startTime())) {
                throw new BadRequestException("endTime must be after startTime for " + item.dayOfWeek());
            }
        });
    }

    private void validateNoSameDayOverlaps(List<RecurringAvailabilityItemRequest> windows) {
        Map<DayOfWeek, List<RecurringAvailabilityItemRequest>> groupedByDay = new HashMap<>();

        for (RecurringAvailabilityItemRequest window : windows) {
            groupedByDay.computeIfAbsent(window.dayOfWeek(), ignored -> new ArrayList<>()).add(window);
        }

        groupedByDay.forEach((day, dayWindows) -> {
            dayWindows.sort(Comparator.comparing(RecurringAvailabilityItemRequest::startTime));

            for (int i = 1; i < dayWindows.size(); i++) {
                RecurringAvailabilityItemRequest previous = dayWindows.get(i - 1);
                RecurringAvailabilityItemRequest current = dayWindows.get(i);

                if (current.startTime().isBefore(previous.endTime())) {
                    throw new BadRequestException("Overlapping recurring availability windows are not allowed for " + day);
                }
            }
        });
    }

    private RecurringAvailabilityResponse toResponse(RecurringAvailability availability) {
        return new RecurringAvailabilityResponse(
                availability.getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime()
        );
    }

    private AvailabilityOverrideResponse toOverrideResponse(AvailabilityOverride override) {
        return new AvailabilityOverrideResponse(
                override.getId(),
                override.getStartDate(),
                override.getEndDate(),
                override.getReason()
        );
    }

    private void validateDateOrder(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("startDate and endDate are required");
        }

        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("endDate must be on or after startDate");
        }
    }

    private record EmployeeDayKey(Long employeeId, DayOfWeek dayOfWeek) {
    }
}


