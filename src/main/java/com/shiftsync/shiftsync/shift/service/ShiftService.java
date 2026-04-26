package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.shift.dto.CreateShiftRequest;
import com.shiftsync.shiftsync.shift.dto.ShiftResponse;

public interface ShiftService {

    /**
     * Creates a new shift for a given location and department.
     *
     * @param actorUserId the ID of the authenticated user creating the shift
     * @param request     the shift creation request
     * @return the created shift response
     */
    ShiftResponse createShift(Long actorUserId, CreateShiftRequest request);

    /**
     * Cancels a shift and notifies all assigned employees.
     *
     * @param actorUserId the ID of the authenticated user performing the cancel
     * @param shiftId     the shift to cancel
     */
    void cancelShift(Long actorUserId, Long shiftId);
}
