package com.shiftsync.shiftsync.location.repository;

import com.shiftsync.shiftsync.location.entity.ManagerLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The interface Manager location repository.
 */
@Repository
public interface ManagerLocationRepository extends JpaRepository<ManagerLocation, Long> {

    /**
     * Find location ids by manager employee id list.
     *
     * @param managerEmployeeId the manager employee id
     * @return the list
     */
    @Query("select ml.location.id from ManagerLocation ml where ml.manager.id = :managerEmployeeId")
    List<Long> findLocationIdsByManagerEmployeeId(Long managerEmployeeId);
}

