package com.shiftsync.shiftsync.availability.repository;

import com.shiftsync.shiftsync.availability.entity.AvailabilityOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The interface Availability override repository.
 */
@Repository
public interface AvailabilityOverrideRepository extends JpaRepository<AvailabilityOverride, Long> {

    /**
     * Exists by employee id and start date less than equal and end date greater than equal boolean.
     *
     * @param employeeId the employee id
     * @param endDate    the end date
     * @param startDate  the start date
     * @return the boolean
     */
    @Query("""
            select (count(o) > 0)
            from AvailabilityOverride o
            where o.employee.id = :employeeId
              and o.startDate <= :endDate
              and o.endDate >= :startDate
            """)
    boolean hasOverlap(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find by employee id and end date greater than equal order by start date asc list.
     *
     * @param employeeId the employee id
     * @param date       the date
     * @return the list
     */
    @Query("""
            select o
            from AvailabilityOverride o
            where o.employee.id = :employeeId
              and o.endDate >= :date
            order by o.startDate asc
            """)
    List<AvailabilityOverride> findActiveByEmployee(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    /**
     * Find by employee id in and start date less than equal and end date greater than equal list.
     *
     * @param employeeIds the employee ids
     * @param endDate     the end date
     * @param startDate   the start date
     * @return the list
     */
    @Query("""
            select o
            from AvailabilityOverride o
            where o.employee.id in :employeeIds
              and o.startDate <= :endDate
              and o.endDate >= :startDate
            """)
    List<AvailabilityOverride> findWeekOverlaps(
            @Param("employeeIds") List<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find by id and employee id optional.
     *
     * @param id         the id
     * @param employeeId the employee id
     * @return the optional
     */
    Optional<AvailabilityOverride> findByIdAndEmployeeId(Long id, Long employeeId);
}

