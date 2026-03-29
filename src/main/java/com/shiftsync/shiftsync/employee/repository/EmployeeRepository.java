package com.shiftsync.shiftsync.employee.repository;

import com.shiftsync.shiftsync.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The interface Employee repository.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /**
     * Exists by user id boolean.
     *
     * @param userId the user id
     * @return the boolean
     */
    boolean existsByUserId(Long userId);

    /**
     * Find by user id optional.
     *
     * @param userId the user id
     * @return the optional
     */
    Optional<Employee> findByUserId(Long userId);
}

