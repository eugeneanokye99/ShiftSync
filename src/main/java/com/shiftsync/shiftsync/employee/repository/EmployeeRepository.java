package com.shiftsync.shiftsync.employee.repository;

import com.shiftsync.shiftsync.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The interface Employee repository.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Exists by user id boolean.
     *
     * @param userId the user id
     * @return the boolean
     */
    boolean existsByUserId(Long userId);
}

