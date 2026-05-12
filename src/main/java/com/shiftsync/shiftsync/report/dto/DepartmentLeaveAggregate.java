package com.shiftsync.shiftsync.report.dto;

public record DepartmentLeaveAggregate(
        String departmentName,
        long annualDaysTaken,
        long sickDaysTaken,
        long unpaidDaysTaken,
        long totalDaysTaken
) {
}
