package com.shiftsync.shiftsync.report.service.impl;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.LeaveType;
import com.shiftsync.shiftsync.leave.entity.LeaveRequest;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.report.dto.CoverageReportEntry;
import com.shiftsync.shiftsync.report.dto.CoverageReportPageResponse;
import com.shiftsync.shiftsync.report.dto.DepartmentLeaveAggregate;
import com.shiftsync.shiftsync.report.dto.LeaveEmployeeEntry;
import com.shiftsync.shiftsync.report.dto.LeaveUtilizationReportResponse;
import com.shiftsync.shiftsync.report.dto.OvertimeReportEntry;
import com.shiftsync.shiftsync.report.dto.OvertimeReportPageResponse;
import com.shiftsync.shiftsync.report.dto.OvertimeShiftEntry;
import com.shiftsync.shiftsync.report.service.ReportService;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.entity.StaffingStatus;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public CoverageReportPageResponse getCoverageReport(Long locationId, LocalDate from, LocalDate to, int page, int size) {
        List<CoverageReportEntry> all = fetchCoverageEntries(locationId, from, to);
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
        return fetchCoverageEntries(locationId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public OvertimeReportPageResponse getOvertimeReport(Long locationId, LocalDate from, LocalDate to, int page, int size) {
        List<OvertimeReportEntry> all = fetchOvertimeEntries(locationId, from, to);
        int totalElements = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int start = page * size;
        List<OvertimeReportEntry> content = start >= totalElements
                ? List.of()
                : all.subList(start, Math.min(start + size, totalElements));
        return new OvertimeReportPageResponse(content, totalElements, totalPages, page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OvertimeReportEntry> getAllOvertimeEntries(Long locationId, LocalDate from, LocalDate to) {
        return fetchOvertimeEntries(locationId, from, to);
    }

    private List<CoverageReportEntry> fetchCoverageEntries(Long locationId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' date must not be after 'to' date");
        }
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
                            .toList();
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
                .toList();
    }

    private List<OvertimeReportEntry> fetchOvertimeEntries(Long locationId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' date must not be after 'to' date");
        }
        List<ShiftAssignment> assignments = shiftAssignmentRepository
                .findAllInDateRangeByOptionalLocation(from, to, ShiftStatus.CANCELLED, locationId);

        long daysInPeriod = ChronoUnit.DAYS.between(from, to) + 1;

        return assignments.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getId()))
                .values()
                .stream()
                .map(employeeAssignments -> {
                    ShiftAssignment first = employeeAssignments.getFirst();
                    BigDecimal contractedWeeklyHours = first.getEmployee().getContractedWeeklyHours();

                    BigDecimal expectedHours = contractedWeeklyHours
                            .multiply(BigDecimal.valueOf(daysInPeriod))
                            .divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

                    BigDecimal actualHours = employeeAssignments.stream()
                            .map(a -> {
                                long minutes = Duration.between(
                                        a.getShift().getStartTime(), a.getShift().getEndTime()
                                ).toMinutes();
                                return BigDecimal.valueOf(minutes)
                                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal overage = actualHours.subtract(expectedHours);
                    if (overage.compareTo(BigDecimal.ZERO) <= 0) {
                        return null;
                    }

                    List<OvertimeShiftEntry> contributingShifts = employeeAssignments.stream()
                            .map(a -> {
                                Shift shift = a.getShift();
                                long minutes = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
                                BigDecimal hours = BigDecimal.valueOf(minutes)
                                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                                return new OvertimeShiftEntry(
                                        shift.getId(),
                                        shift.getShiftDate(),
                                        shift.getStartTime(),
                                        shift.getEndTime(),
                                        shift.getLocation().getName(),
                                        shift.getDepartment().getName(),
                                        hours
                                );
                            })
                            .sorted(Comparator.comparing(OvertimeShiftEntry::date))
                            .toList();

                    return new OvertimeReportEntry(
                            first.getEmployee().getId(),
                            first.getEmployee().getUser().getFullName(),
                            contractedWeeklyHours,
                            actualHours,
                            overage,
                            contributingShifts
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OvertimeReportEntry::overtimeHours).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveUtilizationReportResponse getLeaveReport(Long locationId, LocalDate from, LocalDate to, int page, int size) {
        LeaveUtilizationReportResponse full = fetchLeaveData(locationId, from, to);
        List<LeaveEmployeeEntry> all = full.employees();
        int totalElements = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int start = page * size;
        List<LeaveEmployeeEntry> content = start >= totalElements
                ? List.of()
                : all.subList(start, Math.min(start + size, totalElements));
        return new LeaveUtilizationReportResponse(content, totalElements, totalPages, page, full.departmentAggregates());
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveUtilizationReportResponse getAllLeaveData(Long locationId, LocalDate from, LocalDate to) {
        return fetchLeaveData(locationId, from, to);
    }

    private LeaveUtilizationReportResponse fetchLeaveData(Long locationId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' date must not be after 'to' date");
        }
        List<LeaveRequest> leaveRequests = leaveRequestRepository
                .findByStatusInRangeByOptionalLocation(from, to, LeaveStatus.APPROVED, locationId);

        List<LeaveEmployeeEntry> employees = leaveRequests.stream()
                .collect(Collectors.groupingBy(lr -> lr.getEmployee().getId()))
                .values()
                .stream()
                .map(employeeLeaves -> {
                    LeaveRequest first = employeeLeaves.getFirst();
                    Map<LeaveType, Long> daysByType = employeeLeaves.stream()
                            .collect(Collectors.toMap(
                                    LeaveRequest::getLeaveType,
                                    lr -> calculateDaysInPeriod(lr, from, to),
                                    Long::sum
                            ));
                    long annual = daysByType.getOrDefault(LeaveType.ANNUAL, 0L);
                    long sick = daysByType.getOrDefault(LeaveType.SICK, 0L);
                    long unpaid = daysByType.getOrDefault(LeaveType.UNPAID, 0L);
                    return new LeaveEmployeeEntry(
                            first.getEmployee().getId(),
                            first.getEmployee().getUser().getFullName(),
                            first.getEmployee().getDepartment().getName(),
                            annual,
                            sick,
                            unpaid,
                            annual + sick + unpaid
                    );
                })
                .sorted(Comparator.comparing(LeaveEmployeeEntry::employeeName))
                .toList();

        List<DepartmentLeaveAggregate> aggregates = employees.stream()
                .collect(Collectors.groupingBy(LeaveEmployeeEntry::departmentName))
                .entrySet()
                .stream()
                .map(e -> new DepartmentLeaveAggregate(
                        e.getKey(),
                        e.getValue().stream().mapToLong(LeaveEmployeeEntry::annualDaysTaken).sum(),
                        e.getValue().stream().mapToLong(LeaveEmployeeEntry::sickDaysTaken).sum(),
                        e.getValue().stream().mapToLong(LeaveEmployeeEntry::unpaidDaysTaken).sum(),
                        e.getValue().stream().mapToLong(LeaveEmployeeEntry::totalDaysTaken).sum()
                ))
                .sorted(Comparator.comparing(DepartmentLeaveAggregate::departmentName))
                .toList();

        return new LeaveUtilizationReportResponse(employees, employees.size(), 1, 0, aggregates);
    }

    private long calculateDaysInPeriod(LeaveRequest lr, LocalDate from, LocalDate to) {
        LocalDate effectiveStart = lr.getStartDate().isBefore(from) ? from : lr.getStartDate();
        LocalDate effectiveEnd = lr.getEndDate().isAfter(to) ? to : lr.getEndDate();
        return ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
    }

    private StaffingStatus resolveStaffingStatus(int assignedCount, int minimumHeadcount) {
        if (assignedCount < minimumHeadcount) return StaffingStatus.UNDERSTAFFED;
        if (assignedCount == minimumHeadcount) return StaffingStatus.FULLY_STAFFED;
        return StaffingStatus.OVERSTAFFED;
    }
}
