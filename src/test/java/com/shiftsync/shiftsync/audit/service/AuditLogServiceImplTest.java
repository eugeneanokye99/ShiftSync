package com.shiftsync.shiftsync.audit.service;

import com.shiftsync.shiftsync.audit.dto.AuditLogPageResponse;
import com.shiftsync.shiftsync.audit.entity.AuditLog;
import com.shiftsync.shiftsync.audit.repository.AuditLogRepository;
import com.shiftsync.shiftsync.audit.service.impl.AuditLogServiceImpl;
import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.common.enums.AuditAction;
import com.shiftsync.shiftsync.common.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    private User actor;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        actor = User.builder()
                .id(1L)
                .fullName("Jordan HR")
                .email("jordan@shiftsync.com")
                .role(UserRole.HR_ADMIN)
                .build();

        auditLog = AuditLog.builder()
                .entityType("SHIFT")
                .entityId(10L)
                .action(AuditAction.CREATE)
                .actor(actor)
                .actorRole(UserRole.HR_ADMIN)
                .afterState("{\"id\":10}")
                .build();
    }

    @Test
    void save_PersistsAuditLog() {
        auditLogService.save(auditLog);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo("SHIFT");
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.CREATE);
    }

    @Test
    void query_WithEntityTypeOnly_ReturnsPage() {
        Page<AuditLog> page = new PageImpl<>(List.of(auditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        AuditLogPageResponse response = auditLogService.query("SHIFT", null, null, null, null, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().entityType()).isEqualTo("SHIFT");
        assertThat(response.content().getFirst().actorName()).isEqualTo("Jordan HR");
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.currentPage()).isEqualTo(0);
    }

    @Test
    void query_WithAllFilters_PassesSpecificationToRepository() {
        Page<AuditLog> page = new PageImpl<>(List.of(auditLog));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        AuditLogPageResponse response = auditLogService.query("SHIFT", 10L, 1L, from, to, 0, 10);

        assertThat(response.content()).hasSize(1);
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void query_EmptyResult_ReturnsEmptyPage() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        AuditLogPageResponse response = auditLogService.query("EMPLOYEE", null, null, null, null, 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }

    @Test
    void query_ResponseMapsActorNameAndRole() {
        AuditLog log = AuditLog.builder()
                .entityType("LEAVE_REQUEST")
                .entityId(5L)
                .action(AuditAction.UPDATE)
                .actor(actor)
                .actorRole(UserRole.HR_ADMIN)
                .afterState("{\"status\":\"APPROVED\"}")
                .build();

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        AuditLogPageResponse response = auditLogService.query("LEAVE_REQUEST", null, null, null, null, 0, 20);

        assertThat(response.content().getFirst().actorId()).isEqualTo(1L);
        assertThat(response.content().getFirst().actorName()).isEqualTo("Jordan HR");
        assertThat(response.content().getFirst().actorRole()).isEqualTo(UserRole.HR_ADMIN);
        assertThat(response.content().getFirst().action()).isEqualTo(AuditAction.UPDATE);
    }
}
