package com.shiftsync.shiftsync.audit.dto;

import com.shiftsync.shiftsync.common.enums.AuditAction;
import com.shiftsync.shiftsync.common.enums.UserRole;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String entityType,
        Long entityId,
        AuditAction action,
        Long actorId,
        String actorName,
        UserRole actorRole,
        String beforeState,
        String afterState,
        LocalDateTime createdAt
) {
}
