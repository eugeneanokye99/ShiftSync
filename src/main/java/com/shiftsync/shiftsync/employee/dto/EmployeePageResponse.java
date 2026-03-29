package com.shiftsync.shiftsync.employee.dto;

import java.util.List;

public record EmployeePageResponse(
        List<EmployeeResponse> content,
        long totalElements,
        int totalPages,
        int currentPage
) {
}

