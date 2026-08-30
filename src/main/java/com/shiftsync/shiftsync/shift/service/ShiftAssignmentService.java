package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.audit.annotation.Auditable;
import com.shiftsync.shiftsync.common.enums.AuditAction;
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
    @Auditable(entityType = "SHIFT_ASSIGNMENT", action = AuditAction.CREATE, entityIdParam = 1)
    AssignEmployeeResponse assignEmployee(Long actorUserId, Long shiftId, AssignEmployeeRequest request, boolean override);

    /**
     * Removes an employee from a shift and notifies the employee.
     *
     * @param actorUserId the manager's user ID
     * @param shiftId     the shift ID
     * @param employeeId  the employee ID to remove
     */
    @Auditable(entityType = "SHIFT_ASSIGNMENT", action = AuditAction.DELETE, entityIdParam = 1)
    void removeAssignment(Long actorUserId, Long shiftId, Long employeeId);
}

