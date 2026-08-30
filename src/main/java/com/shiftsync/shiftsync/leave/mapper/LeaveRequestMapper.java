package com.shiftsync.shiftsync.leave.mapper;

import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestResponse;
import com.shiftsync.shiftsync.leave.entity.LeaveRequest;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

@Component
public class LeaveRequestMapper {

    public LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        return new LeaveRequestResponse(
                leaveRequest.getId(),
                leaveRequest.getEmployee().getId(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getLeaveType(),
                leaveRequest.getReason(),
                leaveRequest.getStatus(),
                leaveRequest.getSubmittedAt()
        );
    }

    public PendingLeaveRequestResponse toPendingResponse(LeaveRequest leaveRequest) {
        long daysRequested = ChronoUnit.DAYS.between(leaveRequest.getStartDate(), leaveRequest.getEndDate()) + 1;

        return new PendingLeaveRequestResponse(
                leaveRequest.getId(),
                leaveRequest.getEmployee().getId(),
                leaveRequest.getEmployee().getUser().getFullName(),
                leaveRequest.getEmployee().getDepartment().getName(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getLeaveType(),
                daysRequested,
                leaveRequest.getReason(),
                leaveRequest.getStatus(),
                leaveRequest.getSubmittedAt()
        );
    }
}

