package com.shiftsync.shiftsync.shift.repository;

import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import com.shiftsync.shiftsync.shift.entity.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The interface Shift assignment repository.
 */
@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    boolean existsByShiftIdAndEmployeeId(Long shiftId, Long employeeId);

    /**
     * Returns true if the employee has an overlapping shift assignment on the same date,
     * excluding the given shift itself.
     */
    @Query("""
            select (count(sa) > 0)
            from ShiftAssignment sa
            where sa.employee.id = :employeeId
              and sa.shift.id <> :shiftId
              and sa.shift.shiftDate = :shiftDate
              and sa.shift.startTime < :endTime
              and sa.shift.endTime > :startTime
            """)
    boolean existsOverlappingAssignment(
            @Param("employeeId") Long employeeId,
            @Param("shiftId") Long shiftId,
            @Param("shiftDate") LocalDate shiftDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    Optional<ShiftAssignment> findByShiftIdAndEmployeeId(Long shiftId, Long employeeId);

    @Query("""
            select sa
            from ShiftAssignment sa
            join fetch sa.employee e
            join fetch e.user u
            where sa.shift.id = :shiftId
            """)
    List<ShiftAssignment> findByShiftId(@Param("shiftId") Long shiftId);

    /**
     * Returns all assignments for an employee within a given week range, with shift eagerly joined.
     */
    @Query("""
            select sa
            from ShiftAssignment sa
            join fetch sa.shift s
            where sa.employee.id = :employeeId
              and s.shiftDate >= :weekStart
              and s.shiftDate <= :weekEnd
            """)
    List<ShiftAssignment> findByEmployeeInWeek(
            @Param("employeeId") Long employeeId,
            @Param("weekStart") LocalDate weekStart,
            @Param("weekEnd") LocalDate weekEnd
    );

    @Query("""
            select sa
            from ShiftAssignment sa
            join fetch sa.shift s
            join fetch s.location
            join fetch s.department
            where sa.employee.id = :employeeId
              and s.shiftDate >= :from
              and s.shiftDate <= :to
              and s.status in :statuses
            order by s.shiftDate asc, s.startTime asc
            """)
    List<ShiftAssignment> findByEmployeeInRange(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("statuses") Collection<ShiftStatus> statuses
    );

    @Query("""
            select sa
            from ShiftAssignment sa
            join fetch sa.employee e
            join fetch e.user u
            where sa.shift.id in :shiftIds
              and sa.employee.id <> :excludeEmployeeId
            """)
    List<ShiftAssignment> findColleaguesByShiftIds(
            @Param("shiftIds") Collection<Long> shiftIds,
            @Param("excludeEmployeeId") Long excludeEmployeeId
    );

    @Query("""
            select sa
            from ShiftAssignment sa
            join fetch sa.employee e
            join fetch e.user u
            where sa.shift.id in :shiftIds
            """)
    List<ShiftAssignment> findAssignmentsByShiftIds(@Param("shiftIds") Collection<Long> shiftIds);

    @Query("""
            select (count(sa) > 0)
            from ShiftAssignment sa
            where sa.employee.id = :employeeId
              and sa.shift.id not in :excludedShiftIds
              and sa.shift.shiftDate = :shiftDate
              and sa.shift.startTime < :endTime
              and sa.shift.endTime > :startTime
            """)
    boolean existsConflictExcluding(
            @Param("employeeId") Long employeeId,
            @Param("excludedShiftIds") Collection<Long> excludedShiftIds,
            @Param("shiftDate") LocalDate shiftDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
