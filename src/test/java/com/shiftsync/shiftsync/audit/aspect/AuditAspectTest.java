package com.shiftsync.shiftsync.audit.aspect;

import com.shiftsync.shiftsync.audit.annotation.Auditable;
import com.shiftsync.shiftsync.audit.entity.AuditLog;
import com.shiftsync.shiftsync.audit.service.AuditLogService;
import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.AuditAction;
import com.shiftsync.shiftsync.common.enums.UserRole;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Auditable auditable;

    @InjectMocks
    private AuditAspect auditAspect;

    private User actor;

    @BeforeEach
    void setUp() {
        actor = User.builder()
                .id(1L)
                .fullName("Morgan Manager")
                .email("morgan@shiftsync.com")
                .role(UserRole.MANAGER)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, UserRole role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void audit_SuccessfulExecution_SavesAuditLog() throws Throwable {
        authenticateAs(1L, UserRole.MANAGER);
        when(auditable.entityType()).thenReturn("SHIFT");
        when(auditable.action()).thenReturn(AuditAction.CREATE);
        when(auditable.entityIdParam()).thenReturn(-1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        record ShiftResult(Long id) {}
        ShiftResult result = new ShiftResult(10L);
        when(joinPoint.proceed()).thenReturn(result);
        when(objectMapper.writeValueAsString(result)).thenReturn("{\"id\":10}");

        auditAspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo("SHIFT");
        assertThat(saved.getAction()).isEqualTo(AuditAction.CREATE);
        assertThat(saved.getActor()).isEqualTo(actor);
        assertThat(saved.getActorRole()).isEqualTo(UserRole.MANAGER);
        assertThat(saved.getAfterState()).isEqualTo("{\"id\":10}");
    }

    @Test
    void audit_MethodThrows_NoAuditEntrySaved() throws Throwable {
        authenticateAs(1L, UserRole.MANAGER);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("conflict"));

        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("conflict");

        verify(auditLogService, never()).save(any());
    }

    @Test
    void audit_NoAuthContext_NoAuditEntrySaved() throws Throwable {
        SecurityContextHolder.clearContext();
        when(joinPoint.proceed()).thenReturn("result");

        auditAspect.audit(joinPoint, auditable);

        verify(auditLogService, never()).save(any());
    }

    @Test
    void audit_EntityIdFromParameter_UsesParamIndex() throws Throwable {
        authenticateAs(1L, UserRole.MANAGER);
        when(auditable.entityType()).thenReturn("SHIFT");
        when(auditable.action()).thenReturn(AuditAction.UPDATE);
        when(auditable.entityIdParam()).thenReturn(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, 42L});
        when(joinPoint.proceed()).thenReturn(null);

        auditAspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo(42L);
    }

    @Test
    void audit_DeleteAction_AfterStateIsNull() throws Throwable {
        authenticateAs(1L, UserRole.MANAGER);
        when(auditable.entityType()).thenReturn("SHIFT_ASSIGNMENT");
        when(auditable.action()).thenReturn(AuditAction.DELETE);
        when(auditable.entityIdParam()).thenReturn(1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(joinPoint.getArgs()).thenReturn(new Object[]{1L, 5L, 2L});
        when(joinPoint.proceed()).thenReturn(null);

        auditAspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).save(captor.capture());
        assertThat(captor.getValue().getAfterState()).isNull();
        assertThat(captor.getValue().getEntityId()).isEqualTo(5L);
    }

    @Test
    void audit_ActorNotFoundInRepository_NoAuditEntrySaved() throws Throwable {
        authenticateAs(99L, UserRole.MANAGER);
        when(joinPoint.proceed()).thenReturn("result");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        auditAspect.audit(joinPoint, auditable);

        verify(auditLogService, never()).save(any());
    }

    @Test
    void audit_EntityIdFromReturnValue_ReflectionFallback() throws Throwable {
        authenticateAs(1L, UserRole.HR_ADMIN);
        when(auditable.entityType()).thenReturn("EMPLOYEE");
        when(auditable.action()).thenReturn(AuditAction.CREATE);
        when(auditable.entityIdParam()).thenReturn(-1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

        record EmployeeResult(Long employeeId) {}
        EmployeeResult result = new EmployeeResult(7L);
        when(joinPoint.proceed()).thenReturn(result);
        when(objectMapper.writeValueAsString(result)).thenReturn("{\"employeeId\":7}");

        auditAspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo(7L);
    }
}
