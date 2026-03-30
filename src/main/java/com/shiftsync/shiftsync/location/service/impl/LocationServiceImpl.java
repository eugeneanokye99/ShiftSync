package com.shiftsync.shiftsync.location.service.impl;

import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.location.dto.CreateLocationRequest;
import com.shiftsync.shiftsync.location.dto.LocationResponse;
import com.shiftsync.shiftsync.location.dto.UpdateLocationRequest;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.mapper.LocationMapper;
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


