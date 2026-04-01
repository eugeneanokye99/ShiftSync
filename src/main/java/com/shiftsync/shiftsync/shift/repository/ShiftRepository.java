package com.shiftsync.shiftsync.shift.repository;

import com.shiftsync.shiftsync.shift.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Shift repository.
 */
@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
}

