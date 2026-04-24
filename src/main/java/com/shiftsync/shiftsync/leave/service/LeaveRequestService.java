package com.shiftsync.shiftsync.leave.service;

import com.shiftsync.shiftsync.leave.dto.ApproveLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.CreateLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.GetLeaveRequestsRequest;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestPageResponse;
import com.shiftsync.shiftsync.leave.dto.RejectLeaveRequest;

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
     * Approve leave request leave request response.
     *
     * @param actorUserId    the actor user id
     * @param leaveRequestId the leave request id
     * @param request        the request
     * @return the leave request response
     */
    LeaveRequestResponse approveLeaveRequest(Long actorUserId, Long leaveRequestId, ApproveLeaveRequest request);

    /**
     * Reject leave request leave request response.
     *
     * @param actorUserId    the actor user id
     * @param leaveRequestId the leave request id
     * @param request        the request
     * @return the leave request response
     */
    LeaveRequestResponse rejectLeaveRequest(Long actorUserId, Long leaveRequestId, RejectLeaveRequest request);

    /**
     * Cancel leave request leave request response.
     *
     * @param actorUserId    the actor user id
     * @param leaveRequestId the leave request id
     * @return the leave request response
     */
    LeaveRequestResponse cancelLeaveRequest(Long actorUserId, Long leaveRequestId);

    /**
     * Gets leave requests.
     *
     * @param request the request
     * @return the leave requests
     */
    PendingLeaveRequestPageResponse getLeaveRequests(GetLeaveRequestsRequest request);
}