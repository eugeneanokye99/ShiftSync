package com.shiftsync.shiftsync.availability.repository;

import com.shiftsync.shiftsync.availability.entity.AvailabilityOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvailabilityOverrideRepository extends JpaRepository<AvailabilityOverride, Long> {
}

