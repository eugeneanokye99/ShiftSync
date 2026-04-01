package com.shiftsync.shiftsync.department.repository;

import com.shiftsync.shiftsync.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The interface Department repository.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

	/**
	 * Exists by location id and name ignore case boolean.
	 *
	 * @param locationId the location id
	 * @param name       the name
	 * @return the boolean
	 */
	boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);

	/**
	 * Find all by location id order by name asc list.
	 *
	 * @param locationId the location id
	 * @return the list
	 */
	List<Department> findAllByLocationIdOrderByNameAsc(Long locationId);
}

