package com.shiftsync.shiftsync.employee.entity;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.department.entity.Department;
import com.shiftsync.shiftsync.location.entity.Location;
import org.hibernate.annotations.ColumnTransformer;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::employment_type")
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "skills", columnDefinition = "text[]")
    private String[] skills;

    @Column(name = "contracted_weekly_hours", nullable = false)
    private BigDecimal contractedWeeklyHours;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (hireDate == null) {
            hireDate = LocalDate.now();
        }
        if (active == null) {
            active = true;
        }
        if (notificationEnabled == null) {
            notificationEnabled = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}


