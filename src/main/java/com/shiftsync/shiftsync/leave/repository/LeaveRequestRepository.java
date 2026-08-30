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
import java.util.List;

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

    @Query("""
            select lr
            from LeaveRequest lr
            join fetch lr.employee e
            join fetch e.user u
            join fetch e.department d
            where lr.status = :status
              and lr.startDate <= :to
              and lr.endDate >= :from
              and (:locationId is null or e.location.id = :locationId)
            order by e.id asc
            """)
    List<LeaveRequest> findByStatusInRangeByOptionalLocation(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") LeaveStatus status,
            @Param("locationId") Long locationId
    );
}

