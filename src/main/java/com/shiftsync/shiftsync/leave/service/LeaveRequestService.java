package com.shiftsync.shiftsync.leave.service;

import com.shiftsync.shiftsync.leave.dto.CreateLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.GetPendingLeaveRequestsRequest;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestPageResponse;

/**
 * The interface Leave request service.
 */
public interface LeaveRequestService {

    /**
     * Create leave request leave request response.
     *
     * @param actorUserId the actor user id
     * @param request     the request
     * @return the leave request response
     */
    LeaveRequestResponse createLeaveRequest(Long actorUserId, CreateLeaveRequest request);

    /**
     * Gets pending leave requests.
     *
     * @param request the request
     * @return the pending leave requests
     */
    PendingLeaveRequestPageResponse getPendingLeaveRequests(GetPendingLeaveRequestsRequest request);
}

