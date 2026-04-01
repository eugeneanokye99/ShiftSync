package com.shiftsync.shiftsync.shift.repository;

import com.shiftsync.shiftsync.shift.entity.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Shift assignment repository.
 */
@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    /**
     * Exists by shift id and employee id boolean.
     *
     * @param shiftId    the shift id
     * @param employeeId the employee id
     * @return the boolean
     */
    boolean existsByShiftIdAndEmployeeId(Long shiftId, Long employeeId);
}

