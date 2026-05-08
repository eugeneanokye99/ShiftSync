package com.shiftsync.shiftsync.report.exporter;

import com.shiftsync.shiftsync.report.dto.CoverageReportEntry;
import org.springframework.stereotype.Component;

import java.util.List;

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

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
