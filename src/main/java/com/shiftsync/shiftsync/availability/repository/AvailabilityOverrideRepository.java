package com.shiftsync.shiftsync.availability.repository;

import com.shiftsync.shiftsync.availability.entity.AvailabilityOverride;
import org.springframework.data.jpa.repository.JpaRepository;
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
    boolean existsByEmployeeIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            LocalDate endDate,
            LocalDate startDate
    );

    /**
     * Find by employee id and end date greater than equal order by start date asc list.
     *
     * @param employeeId the employee id
     * @param date       the date
     * @return the list
     */
    List<AvailabilityOverride> findByEmployeeIdAndEndDateGreaterThanEqualOrderByStartDateAsc(Long employeeId, LocalDate date);

    /**
     * Find by id and employee id optional.
     *
     * @param id         the id
     * @param employeeId the employee id
     * @return the optional
     */
    Optional<AvailabilityOverride> findByIdAndEmployeeId(Long id, Long employeeId);
}

