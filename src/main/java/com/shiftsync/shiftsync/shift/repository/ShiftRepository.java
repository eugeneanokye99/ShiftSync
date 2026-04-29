package com.shiftsync.shiftsync.shift.repository;

import com.shiftsync.shiftsync.shift.entity.Shift;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The interface Shift repository.
 */
@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shift s where s.id = :id")
    Optional<Shift> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select s
            from Shift s
            join fetch s.location
            join fetch s.department
            where s.location.id = :locationId
              and s.shiftDate >= :from
              and s.shiftDate <= :to
            order by s.shiftDate asc, s.startTime asc
            """)
    List<Shift> findByLocationInRange(
            @Param("locationId") Long locationId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
