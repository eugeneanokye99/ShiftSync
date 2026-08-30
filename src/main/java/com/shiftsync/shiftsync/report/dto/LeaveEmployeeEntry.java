package com.shiftsync.shiftsync.report.dto;

public record LeaveEmployeeEntry(
        Long employeeId,
        String employeeName,
        String departmentName,
        long annualDaysTaken,
        long sickDaysTaken,
        long unpaidDaysTaken,
        long totalDaysTaken
) {
}
