package com.shiftsync.shiftsync.shift.entity;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;

@Entity
@Table(name = "shift_swaps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSwap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_assignment_id", nullable = false)
    private ShiftAssignment requesterAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_employee_id", nullable = false)
    private Employee targetEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_assignment_id")
    private ShiftAssignment targetAssignment;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::shift_swap_status")
    @Column(name = "status", nullable = false)
    private ShiftSwapStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "manager_note")
    private String managerNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ShiftSwapStatus.PENDING_MANAGER_APPROVAL;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
