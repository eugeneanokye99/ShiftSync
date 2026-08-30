package com.shiftsync.shiftsync.audit.service;

import com.shiftsync.shiftsync.audit.dto.AuditLogPageResponse;
import com.shiftsync.shiftsync.audit.entity.AuditLog;

import java.time.LocalDateTime;

public interface AuditLogService {

    void save(AuditLog auditLog);

    AuditLogPageResponse query(String entityType, Long entityId, Long actorId,
                               LocalDateTime from, LocalDateTime to, int page, int size);
}
