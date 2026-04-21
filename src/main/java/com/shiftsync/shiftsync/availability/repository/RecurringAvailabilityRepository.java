package com.shiftsync.shiftsync.availability.repository;

import com.shiftsync.shiftsync.availability.entity.RecurringAvailability;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

/**
 * The interface Recurring availability repository.
 */
@Repository
public interface RecurringAvailabilityRepository extends JpaRepository<RecurringAvailability, Long> {


    /**
     * Delete by employee id.
     *
     * @param employeeId the employee id
     */
    void deleteByEmployeeId(Long employeeId);

    List<RecurringAvailability> findByEmployeeIdIn(List<Long> employeeIds);

    @Query("""
            select r
            from RecurringAvailability r
            where r.employee.id = :employeeId
              and r.dayOfWeek = :dayOfWeek
            order by r.startTime asc
            """)
    List<RecurringAvailability> findByEmployeeAndDay(
            @Param("employeeId") Long employeeId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek
    );
}

