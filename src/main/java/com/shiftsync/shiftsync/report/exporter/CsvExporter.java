package com.shiftsync.shiftsync.report.exporter;

import com.shiftsync.shiftsync.report.dto.CoverageReportEntry;
import com.shiftsync.shiftsync.report.dto.DepartmentLeaveAggregate;
import com.shiftsync.shiftsync.report.dto.LeaveEmployeeEntry;
import com.shiftsync.shiftsync.report.dto.OvertimeReportEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CsvExporter {

    public String toCoverageCsv(List<CoverageReportEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("Shift ID,Date,Start Time,End Time,Department,Required Headcount,Assigned Count,Staffing Status,Employee Names\n");
        for (CoverageReportEntry entry : entries) {
            sb.append(entry.shiftId()).append(",")
                    .append(entry.date()).append(",")
                    .append(entry.startTime()).append(",")
                    .append(entry.endTime()).append(",")
                    .append(escapeCsv(entry.departmentName())).append(",")
                    .append(entry.requiredHeadcount()).append(",")
                    .append(entry.assignedCount()).append(",")
                    .append(entry.staffingStatus()).append(",")
                    .append(escapeCsv(String.join("; ", entry.employeeNames()))).append("\n");
        }
        return sb.toString();
    }

    public String toOvertimeCsv(List<OvertimeReportEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("Employee ID,Employee Name,Contracted Weekly Hours,Actual Hours In Period,Overtime Hours,Contributing Shifts\n");
        for (OvertimeReportEntry entry : entries) {
            String shiftSummary = entry.contributingShifts().stream()
                    .map(s -> s.date() + " " + s.startTime() + "-" + s.endTime() + " @ " + s.locationName())
                    .collect(Collectors.joining(" | "));
            sb.append(entry.employeeId()).append(",")
                    .append(escapeCsv(entry.employeeName())).append(",")
                    .append(entry.contractedWeeklyHours()).append(",")
                    .append(entry.actualHoursInPeriod()).append(",")
                    .append(entry.overtimeHours()).append(",")
                    .append(escapeCsv(shiftSummary)).append("\n");
        }
        return sb.toString();
    }

    public String toLeaveCsv(List<LeaveEmployeeEntry> employees, List<DepartmentLeaveAggregate> aggregates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Employee ID,Employee Name,Department,Annual Days,Sick Days,Unpaid Days,Total Days\n");
        for (LeaveEmployeeEntry entry : employees) {
            sb.append(entry.employeeId()).append(",")
                    .append(escapeCsv(entry.employeeName())).append(",")
                    .append(escapeCsv(entry.departmentName())).append(",")
                    .append(entry.annualDaysTaken()).append(",")
                    .append(entry.sickDaysTaken()).append(",")
                    .append(entry.unpaidDaysTaken()).append(",")
                    .append(entry.totalDaysTaken()).append("\n");
        }
        sb.append("\nDepartment Aggregates\n");
        sb.append("Department,Annual Days,Sick Days,Unpaid Days,Total Days\n");
        for (DepartmentLeaveAggregate agg : aggregates) {
            sb.append(escapeCsv(agg.departmentName())).append(",")
                    .append(agg.annualDaysTaken()).append(",")
                    .append(agg.sickDaysTaken()).append(",")
                    .append(agg.unpaidDaysTaken()).append(",")
                    .append(agg.totalDaysTaken()).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
