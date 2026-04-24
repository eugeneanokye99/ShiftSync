package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.shift.dto.AssignEmployeeRequest;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeResponse;

/**
 * The interface Shift assignment service.
 */
public interface ShiftAssignmentService {

    /**
     * Assign employee assign employee response.
     *
     * @param actorUserId the actor user id
     * @param shiftId     the shift id
     * @param request     the request
     * @param override    the override
     * @return the assign employee response
     */
    AssignEmployeeResponse assignEmployee(Long actorUserId, Long shiftId, AssignEmployeeRequest request, boolean override);
}

