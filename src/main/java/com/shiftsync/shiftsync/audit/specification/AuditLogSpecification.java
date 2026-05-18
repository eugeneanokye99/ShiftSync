package com.shiftsync.shiftsync.audit.specification;

import com.shiftsync.shiftsync.audit.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class AuditLogSpecification {

    private AuditLogSpecification() {}

    public static Specification<AuditLog> hasEntityType(String entityType) {
        return (root, _, cb) -> cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> hasEntityId(Long entityId) {
        return (root, _, cb) -> cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> hasActorId(Long actorId) {
        return (root, _, cb) -> cb.equal(root.get("actor").get("id"), actorId);
    }

    public static Specification<AuditLog> createdAfter(LocalDateTime from) {
        return (root, _, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> createdBefore(LocalDateTime to) {
        return (root, _, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
