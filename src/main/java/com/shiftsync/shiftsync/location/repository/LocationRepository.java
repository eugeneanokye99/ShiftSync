package com.shiftsync.shiftsync.location.repository;

import com.shiftsync.shiftsync.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The interface Location repository.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

	/**
	 * Exists by name ignore case boolean.
	 *
	 * @param name the name
	 * @return the boolean
	 */
	boolean existsByNameIgnoreCase(String name);

	/**
	 * Exists by name ignore case and id not boolean.
	 *
	 * @param name the name
	 * @param id   the id
	 * @return the boolean
	 */
	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

	/**
	 * Find all by active true list.
	 *
	 * @return the list
	 */
	List<Location> findAllByActiveTrue();
}

