package com.shiftsync.shiftsync.availability.service.impl;

import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityItemRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;
import com.shiftsync.shiftsync.availability.entity.RecurringAvailability;
import com.shiftsync.shiftsync.availability.repository.RecurringAvailabilityRepository;
import com.shiftsync.shiftsync.availability.service.AvailabilityService;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final EmployeeRepository employeeRepository;
    private final RecurringAvailabilityRepository recurringAvailabilityRepository;

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
}


