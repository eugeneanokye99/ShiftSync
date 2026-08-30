package com.shiftsync.shiftsync.location.service.impl;

import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.location.dto.CreateLocationRequest;
import com.shiftsync.shiftsync.location.dto.LocationResponse;
import com.shiftsync.shiftsync.location.entity.ManagerLocation;
import com.shiftsync.shiftsync.location.dto.UpdateLocationRequest;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.mapper.LocationMapper;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.location.repository.LocationRepository;
import com.shiftsync.shiftsync.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagerLocationRepository managerLocationRepository;
    private final LocationMapper locationMapper;

    @Override
    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request) {
        if (locationRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Location name already exists");
        }

        Location location = Location.builder()
                .name(request.name().trim())
                .address(request.address().trim())
                .maxHeadcountPerShift(request.maxHeadcountPerShift())
                .active(true)
                .build();

        Location saved = locationRepository.save(location);
        return locationMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getActiveLocations() {
        return locationRepository.findAllByActiveTrue()
                .stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAssignedLocationsForManager(Long actorUserId) {
        Employee manager = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));

        List<Long> locationIds = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());
        if (locationIds.isEmpty()) {
            return List.of();
        }

        return locationRepository.findAllById(locationIds)
                .stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void assignManagerToLocation(Long managerEmployeeId, Long locationId) {
        Employee manager = employeeRepository.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (!UserRole.MANAGER.equals(manager.getUser().getRole())) {
            throw new InvalidStateException("Employee is not a manager");
        }

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));

        if (managerLocationRepository.existsByManagerIdAndLocationId(managerEmployeeId, locationId)) {
            throw new DuplicateResourceException("Manager is already assigned to this location");
        }

        managerLocationRepository.save(
                ManagerLocation.builder()
                        .manager(manager)
                        .location(location)
                        .build()
        );
    }

    @Override
    @Transactional
    public void unassignManagerFromLocation(Long managerEmployeeId, Long locationId) {
        Employee manager = employeeRepository.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));

        if (!UserRole.MANAGER.equals(manager.getUser().getRole())) {
            throw new InvalidStateException("Employee is not a manager");
        }

        locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));

        ManagerLocation managerLocation = managerLocationRepository.findByManagerIdAndLocationId(managerEmployeeId, locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager assignment not found"));

        managerLocationRepository.delete(managerLocation);
    }

    @Override
    @Transactional
    public LocationResponse updateLocation(Long locationId, UpdateLocationRequest request) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));

        if (request.name() != null) {
            String normalizedName = request.name().trim();
            if (locationRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, locationId)) {
                throw new DuplicateResourceException("Location name already exists");
            }
            location.setName(normalizedName);
        }

        if (request.address() != null) {
            String normalizedAddress = request.address().trim();
            location.setAddress(normalizedAddress);
        }

        if (request.maxHeadcountPerShift() != null) {
            location.setMaxHeadcountPerShift(request.maxHeadcountPerShift());
        }

        if (request.active() != null) {
            location.setActive(request.active());
        }

        Location saved = locationRepository.save(location);
        return locationMapper.toResponse(saved);
    }
}


