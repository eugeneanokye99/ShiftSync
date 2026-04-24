package com.shiftsync.shiftsync.leave.repository;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.leave.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;

/**
 * The interface Leave request repository.
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    /**
     * Exists overlapping by employee and statuses boolean.
     *
     * @param employeeId the employee id
     * @param startDate  the start date
     * @param endDate    the end date
     * @param statuses   the statuses
     * @return the boolean
     */
    @Query("""
            select case when count(lr) > 0 then true else false end
            from LeaveRequest lr
            where lr.employee.id = :employeeId
              and lr.status in :statuses
              and lr.startDate <= :endDate
              and lr.endDate >= :startDate
            """)
    boolean existsOverlappingByEmployeeAndStatuses(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<LeaveStatus> statuses
    );
}

