package com.shiftsync.shiftsync.report.service.impl;

import com.shiftsync.shiftsync.report.dto.CoverageReportEntry;
import com.shiftsync.shiftsync.report.dto.CoverageReportPageResponse;
import com.shiftsync.shiftsync.report.service.ReportService;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.entity.StaffingStatus;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;

    @Override
    @Transactional(readOnly = true)
    public CoverageReportPageResponse getCoverageReport(Long locationId, LocalDate from, LocalDate to, int page, int size) {
        List<CoverageReportEntry> all = getAllCoverageEntries(locationId, from, to);
        int totalElements = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int start = page * size;
        List<CoverageReportEntry> content = start >= totalElements
                ? List.of()
                : all.subList(start, Math.min(start + size, totalElements));
        return new CoverageReportPageResponse(content, totalElements, totalPages, page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoverageReportEntry> getAllCoverageEntries(Long locationId, LocalDate from, LocalDate to) {
        List<Shift> shifts = shiftRepository.findByLocationInRange(locationId, from, to)
                .stream()
                .filter(s -> s.getStatus() != ShiftStatus.CANCELLED)
                .toList();

        if (shifts.isEmpty()) {
            return List.of();
        }

        Set<Long> shiftIds = shifts.stream().map(Shift::getId).collect(Collectors.toSet());

        Map<Long, List<ShiftAssignment>> assignmentsByShift = shiftAssignmentRepository
                .findAssignmentsByShiftIds(shiftIds)
                .stream()
                .collect(Collectors.groupingBy(a -> a.getShift().getId()));

        return shifts.stream()
                .map(shift -> {
                    List<ShiftAssignment> assignments = assignmentsByShift.getOrDefault(shift.getId(), List.of());
                    int assignedCount = assignments.size();
                    List<String> employeeNames = assignments.stream()
                            .map(a -> a.getEmployee().getUser().getFullName())
                            .collect(Collectors.toList());
                    return new CoverageReportEntry(
                            shift.getId(),
                            shift.getShiftDate(),
                            shift.getStartTime(),
                            shift.getEndTime(),
                            shift.getDepartment().getName(),
                            shift.getMinimumHeadcount(),
                            assignedCount,
                            resolveStaffingStatus(assignedCount, shift.getMinimumHeadcount()),
                            employeeNames
                    );
                })
                .collect(Collectors.toList());
    }

    private StaffingStatus resolveStaffingStatus(int assignedCount, int minimumHeadcount) {
        if (assignedCount < minimumHeadcount) return StaffingStatus.UNDERSTAFFED;
        if (assignedCount == minimumHeadcount) return StaffingStatus.FULLY_STAFFED;
        return StaffingStatus.OVERSTAFFED;
    }
}
