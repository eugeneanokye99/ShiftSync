package com.shiftsync.shiftsync.audit.service.impl;

import com.shiftsync.shiftsync.audit.dto.AuditLogPageResponse;
import com.shiftsync.shiftsync.audit.dto.AuditLogResponse;
import com.shiftsync.shiftsync.audit.entity.AuditLog;
import com.shiftsync.shiftsync.audit.repository.AuditLogRepository;
import com.shiftsync.shiftsync.audit.service.AuditLogService;
import com.shiftsync.shiftsync.audit.specification.AuditLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void save(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogPageResponse query(String entityType, Long entityId, Long actorId,
                                      LocalDateTime from, LocalDateTime to, int page, int size) {
        Specification<AuditLog> spec = Specification.where(AuditLogSpecification.hasEntityType(entityType));

        if (entityId != null) {
            spec = spec.and(AuditLogSpecification.hasEntityId(entityId));
        }
        if (actorId != null) {
            spec = spec.and(AuditLogSpecification.hasActorId(actorId));
        }
        if (from != null) {
            spec = spec.and(AuditLogSpecification.createdAfter(from));
        }
        if (to != null) {
            spec = spec.and(AuditLogSpecification.createdBefore(to));
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);

        return new AuditLogPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber()
        );
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getActor().getId(),
                log.getActor().getFullName(),
                log.getActorRole(),
                log.getBeforeState(),
                log.getAfterState(),
                log.getCreatedAt()
        );
    }
}
