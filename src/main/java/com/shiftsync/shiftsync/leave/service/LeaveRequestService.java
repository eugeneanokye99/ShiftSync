package com.shiftsync.shiftsync.leave.service;

import com.shiftsync.shiftsync.leave.dto.CreateLeaveRequest;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;

public interface LeaveRequestService {

    LeaveRequestResponse createLeaveRequest(Long actorUserId, CreateLeaveRequest request);
}

