package com.shiftsync.shiftsync.availability.service;

import com.shiftsync.shiftsync.availability.dto.AvailabilityOverrideResponse;
import com.shiftsync.shiftsync.availability.dto.CreateAvailabilityOverrideRequest;
import com.shiftsync.shiftsync.availability.dto.ManagerWeeklyAvailabilityResponse;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityItemRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;

import java.time.LocalDate;
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

    /**
     * Create override availability override response.
     *
     * @param actorUserId the actor user id
     * @param request     the request
     * @return the availability override response
     */
    AvailabilityOverrideResponse createOverride(Long actorUserId, CreateAvailabilityOverrideRequest request);

    /**
     * List active overrides list.
     *
     * @param actorUserId the actor user id
     * @return the list
     */
    List<AvailabilityOverrideResponse> listActiveOverrides(Long actorUserId);

    /**
     * Delete override.
     *
     * @param actorUserId the actor user id
     * @param overrideId  the override id
     */
    void deleteOverride(Long actorUserId, Long overrideId);

    /**
     * Gets location weekly availability.
     *
     * @param actorUserId the actor user id
     * @param locationId  the location id
     * @param weekDate    the week date
     * @return the location weekly availability
     */
    ManagerWeeklyAvailabilityResponse getLocationWeeklyAvailability(Long actorUserId, Long locationId, LocalDate weekDate);
}

