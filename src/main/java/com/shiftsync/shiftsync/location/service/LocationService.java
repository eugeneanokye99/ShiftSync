package com.shiftsync.shiftsync.location.service;

import com.shiftsync.shiftsync.location.dto.CreateLocationRequest;
import com.shiftsync.shiftsync.location.dto.LocationResponse;
import com.shiftsync.shiftsync.location.dto.UpdateLocationRequest;

import java.util.List;

/**
 * The interface Location service.
 */
public interface LocationService {

    /**
     * Create location location response.
     *
     * @param request the request
     * @return the location response
     */
    LocationResponse createLocation(CreateLocationRequest request);

    /**
     * Gets active locations.
     *
     * @return the active locations
     */
    List<LocationResponse> getActiveLocations();

    /**
     * Gets assigned locations for manager.
     *
     * @param actorUserId the actor user id
     * @return the assigned locations for manager
     */
    List<LocationResponse> getAssignedLocationsForManager(Long actorUserId);

    /**
     * Update location location response.
     *
     * @param locationId the location id
     * @param request    the request
     * @return the location response
     */
    LocationResponse updateLocation(Long locationId, UpdateLocationRequest request);
}

