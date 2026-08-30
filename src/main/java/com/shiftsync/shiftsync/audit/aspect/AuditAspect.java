package com.shiftsync.shiftsync.audit.aspect;

import com.shiftsync.shiftsync.audit.annotation.Auditable;
import com.shiftsync.shiftsync.audit.entity.AuditLog;
import com.shiftsync.shiftsync.audit.service.AuditLogService;
import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.AuditAction;
import com.shiftsync.shiftsync.common.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint jp, Auditable auditable) throws Throwable {
        Object result = jp.proceed();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long actorId)) {
            return result;
        }

        Optional<User> actorOpt = userRepository.findById(actorId);
        if (actorOpt.isEmpty()) {
            return result;
        }

        UserRole actorRole = extractRole(auth);
        Long entityId = resolveEntityId(jp.getArgs(), auditable, result);
        String afterState = resolveAfterState(auditable.action(), result);

        AuditLog auditLog = AuditLog.builder()
                .entityType(auditable.entityType())
                .entityId(entityId)
                .action(auditable.action())
                .actor(actorOpt.get())
                .actorRole(actorRole)
                .afterState(afterState)
                .build();

        auditLogService.save(auditLog);

        return result;
    }

    private UserRole extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.replace("ROLE_", ""))
                .map(UserRole::valueOf)
                .findFirst()
                .orElse(UserRole.EMPLOYEE);
    }

    private Long resolveEntityId(Object[] args, Auditable auditable, Object result) {
        if (auditable.entityIdParam() >= 0 && args != null && auditable.entityIdParam() < args.length) {
            Object param = args[auditable.entityIdParam()];
            if (param instanceof Long id) {
                return id;
            }
        }
        return extractIdFromResult(result);
    }

    private Long extractIdFromResult(Object result) {
        if (result == null) {
            return null;
        }
        for (String methodName : new String[]{"getId", "id", "employeeId", "assignmentId"}) {
            try {
                Object value = result.getClass().getMethod(methodName).invoke(result);
                if (value instanceof Long id) {
                    return id;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String resolveAfterState(AuditAction action, Object result) {
        if (action == AuditAction.DELETE || result == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception ignored) {
            return null;
        }
    }
}
