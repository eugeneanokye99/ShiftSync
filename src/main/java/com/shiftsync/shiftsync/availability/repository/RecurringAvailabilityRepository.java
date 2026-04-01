package com.shiftsync.shiftsync.availability.repository;

import com.shiftsync.shiftsync.availability.entity.RecurringAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}

