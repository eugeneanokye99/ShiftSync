package com.shiftsync.shiftsync.report.dto;

import java.util.List;

public record LeaveUtilizationReportResponse(
        List<LeaveEmployeeEntry> employees,
        int totalElements,
        int totalPages,
        int page,
        List<DepartmentLeaveAggregate> departmentAggregates
) {
}
