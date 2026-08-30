package com.shiftsync.shiftsync.audit.dto;

import java.util.List;

public record AuditLogPageResponse(
        List<AuditLogResponse> content,
        long totalElements,
        int totalPages,
        int currentPage
) {
}
