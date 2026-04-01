package com.shiftsync.shiftsync.availability.service;

import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityItemRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;

import java.util.List;

/**
 * The interface Availability service.
 */
public interface AvailabilityService {

    /**
     * Replace recurring availability list.
     *
     * @param actorUserId the actor user id
     * @param windows     the windows
     * @return the list
     */
    List<RecurringAvailabilityResponse> replaceRecurringAvailability(
            Long actorUserId,
            List<RecurringAvailabilityItemRequest> windows
    );
}

