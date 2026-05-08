package com.shiftsync.shiftsync.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record OvertimeReportEntry(
        Long employeeId,
        String employeeName,
        BigDecimal contractedWeeklyHours,
        BigDecimal actualHoursInPeriod,
        BigDecimal overtimeHours,
        List<OvertimeShiftEntry> contributingShifts
) {
}
