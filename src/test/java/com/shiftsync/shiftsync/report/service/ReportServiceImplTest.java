package com.shiftsync.shiftsync.report.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.LeaveType;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.department.entity.Department;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.leave.entity.LeaveRequest;
import com.shiftsync.shiftsync.leave.repository.LeaveRequestRepository;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.report.dto.CoverageReportEntry;
import com.shiftsync.shiftsync.report.dto.CoverageReportPageResponse;
import com.shiftsync.shiftsync.report.dto.DepartmentLeaveAggregate;
import com.shiftsync.shiftsync.report.dto.LeaveEmployeeEntry;
import com.shiftsync.shiftsync.report.dto.LeaveUtilizationReportResponse;
import com.shiftsync.shiftsync.report.dto.OvertimeReportEntry;
import com.shiftsync.shiftsync.report.dto.OvertimeReportPageResponse;
import com.shiftsync.shiftsync.report.dto.OvertimeShiftEntry;
import com.shiftsync.shiftsync.report.service.impl.ReportServiceImpl;
import com.shiftsync.shiftsync.shift.entity.Shift;
import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import com.shiftsync.shiftsync.shift.entity.StaffingStatus;
import com.shiftsync.shiftsync.shift.repository.ShiftAssignmentRepository;
import com.shiftsync.shiftsync.shift.repository.ShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ShiftRepository shiftRepository;
    @Mock private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks private ReportServiceImpl reportService;

    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO   = LocalDate.of(2026, 6, 7);

    private User user;
    private Employee employee;
    private Location location;
    private Department department;
    private Shift shift;

    @BeforeEach
    void setUp() {
        user       = User.builder().id(1L).fullName("Alice").role(UserRole.EMPLOYEE).build();
        location   = Location.builder().id(10L).name("HQ").address("Accra").maxHeadcountPerShift(10).active(true).build();
        department = Department.builder().id(20L).name("Kitchen").location(location).build();
        employee   = buildEmployee(100L, user);
        shift      = buildShift(50L, FROM, LocalTime.of(9, 0), LocalTime.of(17, 0), 2);
    }

    // ── Coverage report ───────────────────────────────────────────────────────

    @Test
    void getCoverageReport_WithOpenShiftsAndAssignees_ReturnsCorrectEntry() {
        ShiftAssignment assignment = buildAssignment(1L, shift, employee);

        when(shiftRepository.findActiveByLocationInRange(eq(10L), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(shift)));
        when(shiftAssignmentRepository.findAssignmentsByShiftIds(any())).thenReturn(List.of(assignment));

        CoverageReportPageResponse response = reportService.getCoverageReport(10L, FROM, TO, 0, 20);

        assertThat(response.totalElements()).isEqualTo(1);
        CoverageReportEntry entry = response.content().getFirst();
        assertThat(entry.shiftId()).isEqualTo(50L);
        assertThat(entry.departmentName()).isEqualTo("Kitchen");
        assertThat(entry.requiredHeadcount()).isEqualTo(2);
        assertThat(entry.assignedCount()).isEqualTo(1);
        assertThat(entry.employeeNames()).containsExactly("Alice");
        assertThat(entry.staffingStatus()).isEqualTo(StaffingStatus.UNDERSTAFFED);
    }

    @Test
    void getCoverageReport_NoActiveShifts_ReturnsEmptyPage() {
        when(shiftRepository.findActiveByLocationInRange(eq(10L), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        CoverageReportPageResponse response = reportService.getCoverageReport(10L, FROM, TO, 0, 20);

        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.content()).isEmpty();
        assertThat(response.totalPages()).isEqualTo(0);
    }

    @Test
    void getCoverageReport_ReturnsPageMetadataFromRepository() {
        Shift s2 = buildShift(51L, LocalDate.of(2026, 6, 2), LocalTime.of(10, 0), LocalTime.of(18, 0), 1);

        when(shiftRepository.findActiveByLocationInRange(eq(10L), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(shift, s2), PageRequest.of(0, 2), 5));
        when(shiftAssignmentRepository.findAssignmentsByShiftIds(any())).thenReturn(List.of());

        CoverageReportPageResponse response = reportService.getCoverageReport(10L, FROM, TO, 0, 2);

        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void getCoverageReport_AssignedEqualsMinimum_ReturnsFullyStaffed() {
        User u2 = User.builder().id(2L).fullName("Bob").role(UserRole.EMPLOYEE).build();
        ShiftAssignment a1 = buildAssignment(1L, shift, employee);
        ShiftAssignment a2 = buildAssignment(2L, shift, buildEmployee(200L, u2));

        when(shiftRepository.findActiveByLocationInRange(eq(10L), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(shift)));
        when(shiftAssignmentRepository.findAssignmentsByShiftIds(any())).thenReturn(List.of(a1, a2));

        CoverageReportPageResponse response = reportService.getCoverageReport(10L, FROM, TO, 0, 20);

        assertThat(response.content().getFirst().staffingStatus()).isEqualTo(StaffingStatus.FULLY_STAFFED);
    }

    @Test
    void getCoverageReport_AssignedExceedsMinimum_ReturnsOverstaffed() {
        User u2 = User.builder().id(2L).fullName("Bob").role(UserRole.EMPLOYEE).build();
        User u3 = User.builder().id(3L).fullName("Carol").role(UserRole.EMPLOYEE).build();
        ShiftAssignment a1 = buildAssignment(1L, shift, employee);
        ShiftAssignment a2 = buildAssignment(2L, shift, buildEmployee(200L, u2));
        ShiftAssignment a3 = buildAssignment(3L, shift, buildEmployee(300L, u3));

        when(shiftRepository.findActiveByLocationInRange(eq(10L), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(shift)));
        when(shiftAssignmentRepository.findAssignmentsByShiftIds(any())).thenReturn(List.of(a1, a2, a3));

        CoverageReportPageResponse response = reportService.getCoverageReport(10L, FROM, TO, 0, 20);

        assertThat(response.content().getFirst().staffingStatus()).isEqualTo(StaffingStatus.OVERSTAFFED);
        assertThat(response.content().getFirst().assignedCount()).isEqualTo(3);
    }

    @Test
    void getCoverageReport_FromAfterTo_ThrowsBadRequest() {
        assertThatThrownBy(() -> reportService.getCoverageReport(10L, TO, FROM, 0, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'from' date must not be after 'to' date");
    }

    @Test
    void getAllCoverageEntries_ReturnsAllEntriesWithoutPagination() {
        when(shiftRepository.findByLocationInRange(10L, FROM, TO)).thenReturn(List.of(shift));
        when(shiftAssignmentRepository.findAssignmentsByShiftIds(any())).thenReturn(List.of());

        List<CoverageReportEntry> entries = reportService.getAllCoverageEntries(10L, FROM, TO);

        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().shiftId()).isEqualTo(50L);
    }

    // ── Overtime report ───────────────────────────────────────────────────────

    @Test
    void getOvertimeReport_EmployeeExceedsContractedHours_ReturnsOvertimeEntry() {
        // contractedWeeklyHours=10, period=7 days → expected=10h; two 8h shifts=16h → overage=6h
        Shift s2 = buildShift(51L, LocalDate.of(2026, 6, 2), LocalTime.of(9, 0), LocalTime.of(17, 0), 1);
        ShiftAssignment a1 = buildAssignment(1L, shift, employee);
        ShiftAssignment a2 = buildAssignment(2L, s2, employee);

        when(shiftAssignmentRepository.findAllInDateRangeByOptionalLocation(FROM, TO, null))
                .thenReturn(List.of(a1, a2));

        OvertimeReportPageResponse response = reportService.getOvertimeReport(null, FROM, TO, 0, 20);

        assertThat(response.totalElements()).isEqualTo(1);
        OvertimeReportEntry entry = response.content().getFirst();
        assertThat(entry.employeeName()).isEqualTo("Alice");
        assertThat(entry.contractedWeeklyHours()).isEqualByComparingTo("10.00");
        assertThat(entry.actualHoursInPeriod()).isEqualByComparingTo("16.00");
        assertThat(entry.overtimeHours()).isGreaterThan(BigDecimal.ZERO);
        assertThat(entry.contributingShifts()).hasSize(2);
    }

    @Test
    void getOvertimeReport_EmployeeWithinContractedHours_ExcludedFromReport() {
        // contractedWeeklyHours=10, period=7 days → expected=10h; one 8h shift=8h < 10h
        ShiftAssignment a1 = buildAssignment(1L, shift, employee);

        when(shiftAssignmentRepository.findAllInDateRangeByOptionalLocation(FROM, TO, null))
                .thenReturn(List.of(a1));

        OvertimeReportPageResponse response = reportService.getOvertimeReport(null, FROM, TO, 0, 20);

        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.content()).isEmpty();
    }

    @Test
    void getOvertimeReport_SortedByOvertimeHoursDescending() {
        // Alice: 3 × 8h = 24h → overage 14h; Bob: 2 × 8h = 16h → overage 6h
        User userB = User.builder().id(2L).fullName("Bob").role(UserRole.EMPLOYEE).build();
        Employee employeeB = buildEmployee(200L, userB);
        Shift s2 = buildShift(51L, LocalDate.of(2026, 6, 2), LocalTime.of(9, 0), LocalTime.of(17, 0), 1);
        Shift s3 = buildShift(52L, LocalDate.of(2026, 6, 3), LocalTime.of(9, 0), LocalTime.of(17, 0), 1);
        Shift s4 = buildShift(53L, LocalDate.of(2026, 6, 4), LocalTime.of(9, 0), LocalTime.of(17, 0), 1);

        when(shiftAssignmentRepository.findAllInDateRangeByOptionalLocation(FROM, TO, null))
                .thenReturn(List.of(
                        buildAssignment(1L, shift, employee),
                        buildAssignment(2L, s2, employee),
                        buildAssignment(3L, s3, employee),
                        buildAssignment(4L, s3, employeeB),
                        buildAssignment(5L, s4, employeeB)
                ));

        OvertimeReportPageResponse response = reportService.getOvertimeReport(null, FROM, TO, 0, 20);

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content().get(0).employeeName()).isEqualTo("Alice");
        assertThat(response.content().get(1).employeeName()).isEqualTo("Bob");
        assertThat(response.content().get(0).overtimeHours())
                .isGreaterThan(response.content().get(1).overtimeHours());
    }

    @Test
    void getOvertimeReport_NoAssignments_ReturnsEmptyPage() {
        when(shiftAssignmentRepository.findAllInDateRangeByOptionalLocation(FROM, TO, null))
                .thenReturn(List.of());

        OvertimeReportPageResponse response = reportService.getOvertimeReport(null, FROM, TO, 0, 20);

        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.content()).isEmpty();
    }

    @Test
    void getOvertimeReport_ContributingShiftsSortedByDate() {
        Shift earlier = buildShift(51L, LocalDate.of(2026, 6, 3), LocalTime.of(9, 0), LocalTime.of(17, 0), 1);
        Shift later   = buildShift(52L, LocalDate.of(2026, 6, 5), LocalTime.of(9, 0), LocalTime.of(17, 0), 1);

        when(shiftAssignmentRepository.findAllInDateRangeByOptionalLocation(FROM, TO, null))
                .thenReturn(List.of(
                        buildAssignment(1L, shift, employee),
                        buildAssignment(2L, later, employee),
                        buildAssignment(3L, earlier, employee)
                ));

        OvertimeReportPageResponse response = reportService.getOvertimeReport(null, FROM, TO, 0, 20);

        List<OvertimeShiftEntry> shifts = response.content().getFirst().contributingShifts();
        assertThat(shifts.get(0).date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(shifts.get(1).date()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(shifts.get(2).date()).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    void getOvertimeReport_FromAfterTo_ThrowsBadRequest() {
        assertThatThrownBy(() -> reportService.getOvertimeReport(null, TO, FROM, 0, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'from' date must not be after 'to' date");
    }

    // ── Leave utilization report ──────────────────────────────────────────────

    @Test
    void getLeaveReport_ApprovedAnnualLeave_ReturnsCorrectDayCount() {
        // Leave: FROM (2026-06-01) to 2026-06-05 = 5 days
        LeaveRequest leave = buildLeave(1L, employee, FROM, TO.minusDays(2), LeaveType.ANNUAL);

        when(leaveRequestRepository.findByStatusInRangeByOptionalLocation(FROM, TO, LeaveStatus.APPROVED, null))
                .thenReturn(List.of(leave));

        LeaveUtilizationReportResponse response = reportService.getLeaveReport(null, FROM, TO, 0, 20);

        assertThat(response.employees()).hasSize(1);
        LeaveEmployeeEntry entry = response.employees().getFirst();
        assertThat(entry.employeeName()).isEqualTo("Alice");
        assertThat(entry.departmentName()).isEqualTo("Kitchen");
        assertThat(entry.annualDaysTaken()).isEqualTo(5);
        assertThat(entry.sickDaysTaken()).isEqualTo(0);
        assertThat(entry.totalDaysTaken()).isEqualTo(5);
    }

    @Test
    void getLeaveReport_LeaveStartsBeforePeriod_ClipsToPeriodStart() {
        // Leave: 2026-05-29 to 2026-06-04; period: 2026-06-01 to 2026-06-07
        // Effective: 2026-06-01 to 2026-06-04 = 4 days
        LeaveRequest leave = buildLeave(1L, employee, FROM.minusDays(3), FROM.plusDays(3), LeaveType.SICK);

        when(leaveRequestRepository.findByStatusInRangeByOptionalLocation(FROM, TO, LeaveStatus.APPROVED, null))
                .thenReturn(List.of(leave));

        LeaveUtilizationReportResponse response = reportService.getLeaveReport(null, FROM, TO, 0, 20);

        assertThat(response.employees().getFirst().sickDaysTaken()).isEqualTo(4);
    }

    @Test
    void getLeaveReport_MultipleLeaveTypes_SummedPerType() {
        // ANNUAL: 2026-06-01 to 2026-06-02 = 2 days
        // SICK:   2026-06-04 to 2026-06-05 = 2 days
        // UNPAID: 2026-06-06 to 2026-06-06 = 1 day
        LeaveRequest annual = buildLeave(1L, employee, FROM, FROM.plusDays(1), LeaveType.ANNUAL);
        LeaveRequest sick   = buildLeave(2L, employee, FROM.plusDays(3), FROM.plusDays(4), LeaveType.SICK);
        LeaveRequest unpaid = buildLeave(3L, employee, FROM.plusDays(5), FROM.plusDays(5), LeaveType.UNPAID);

        when(leaveRequestRepository.findByStatusInRangeByOptionalLocation(FROM, TO, LeaveStatus.APPROVED, null))
                .thenReturn(List.of(annual, sick, unpaid));

        LeaveEmployeeEntry entry = reportService.getLeaveReport(null, FROM, TO, 0, 20).employees().getFirst();

        assertThat(entry.annualDaysTaken()).isEqualTo(2);
        assertThat(entry.sickDaysTaken()).isEqualTo(2);
        assertThat(entry.unpaidDaysTaken()).isEqualTo(1);
        assertThat(entry.totalDaysTaken()).isEqualTo(5);
    }

    @Test
    void getLeaveReport_DepartmentAggregatesIncludeAllEmployees() {
        User userB = User.builder().id(2L).fullName("Bob").role(UserRole.EMPLOYEE).build();
        Employee employeeB = buildEmployee(200L, userB);

        // Alice: 2 days ANNUAL; Bob: 2 days SICK
        LeaveRequest leaveA = buildLeave(1L, employee,  FROM, FROM.plusDays(1), LeaveType.ANNUAL);
        LeaveRequest leaveB = buildLeave(2L, employeeB, FROM.plusDays(2), FROM.plusDays(3), LeaveType.SICK);

        when(leaveRequestRepository.findByStatusInRangeByOptionalLocation(FROM, TO, LeaveStatus.APPROVED, null))
                .thenReturn(List.of(leaveA, leaveB));

        LeaveUtilizationReportResponse response = reportService.getLeaveReport(null, FROM, TO, 0, 20);

        assertThat(response.departmentAggregates()).hasSize(1);
        DepartmentLeaveAggregate agg = response.departmentAggregates().getFirst();
        assertThat(agg.departmentName()).isEqualTo("Kitchen");
        assertThat(agg.annualDaysTaken()).isEqualTo(2);
        assertThat(agg.sickDaysTaken()).isEqualTo(2);
        assertThat(agg.totalDaysTaken()).isEqualTo(4);
    }

    @Test
    void getLeaveReport_DepartmentAggregatesAlwaysFullRegardlessOfPage() {
        User userB = User.builder().id(2L).fullName("Bob").role(UserRole.EMPLOYEE).build();
        User userC = User.builder().id(3L).fullName("Carol").role(UserRole.EMPLOYEE).build();
        LeaveRequest leaveA = buildLeave(1L, employee,               FROM, FROM, LeaveType.ANNUAL);
        LeaveRequest leaveB = buildLeave(2L, buildEmployee(200L, userB), FROM, FROM, LeaveType.ANNUAL);
        LeaveRequest leaveC = buildLeave(3L, buildEmployee(300L, userC), FROM, FROM, LeaveType.ANNUAL);

        when(leaveRequestRepository.findByStatusInRangeByOptionalLocation(FROM, TO, LeaveStatus.APPROVED, null))
                .thenReturn(List.of(leaveA, leaveB, leaveC));

        LeaveUtilizationReportResponse page0 = reportService.getLeaveReport(null, FROM, TO, 0, 2);
        LeaveUtilizationReportResponse page1 = reportService.getLeaveReport(null, FROM, TO, 1, 2);

        assertThat(page0.employees()).hasSize(2);
        assertThat(page0.totalElements()).isEqualTo(3);
        assertThat(page0.totalPages()).isEqualTo(2);
        assertThat(page1.employees()).hasSize(1);
        assertThat(page0.departmentAggregates()).hasSize(1);
        assertThat(page1.departmentAggregates()).hasSize(1);
    }

    @Test
    void getLeaveReport_NoApprovedLeave_ReturnsEmptyResponse() {
        when(leaveRequestRepository.findByStatusInRangeByOptionalLocation(FROM, TO, LeaveStatus.APPROVED, null))
                .thenReturn(List.of());

        LeaveUtilizationReportResponse response = reportService.getLeaveReport(null, FROM, TO, 0, 20);

        assertThat(response.employees()).isEmpty();
        assertThat(response.departmentAggregates()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }

    @Test
    void getLeaveReport_FromAfterTo_ThrowsBadRequest() {
        assertThatThrownBy(() -> reportService.getLeaveReport(null, TO, FROM, 0, 20))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'from' date must not be after 'to' date");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Shift buildShift(Long id, LocalDate date, LocalTime start, LocalTime end, int minHeadcount) {
        return Shift.builder()
                .id(id).location(location).department(department)
                .shiftDate(date).startTime(start).endTime(end)
                .minimumHeadcount(minHeadcount).status(ShiftStatus.OPEN)
                .createdBy(user)
                .build();
    }

    private ShiftAssignment buildAssignment(Long id, Shift s, Employee emp) {
        return ShiftAssignment.builder()
                .id(id).shift(s).employee(emp).assignedBy(user).overrideApplied(false)
                .build();
    }

    private Employee buildEmployee(Long id, User u) {
        return Employee.builder()
                .id(id).user(u)
                .location(location).department(department)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("10.00"))
                .hireDate(LocalDate.of(2025, 1, 1))
                .active(true).notificationEnabled(true)
                .build();
    }

    private LeaveRequest buildLeave(Long id, Employee emp, LocalDate start, LocalDate end, LeaveType type) {
        return LeaveRequest.builder()
                .id(id).employee(emp)
                .startDate(start).endDate(end)
                .leaveType(type).status(LeaveStatus.APPROVED)
                .build();
    }
}
