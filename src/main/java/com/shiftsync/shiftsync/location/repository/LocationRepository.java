package com.shiftsync.shiftsync.location.repository;

import com.shiftsync.shiftsync.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
}

