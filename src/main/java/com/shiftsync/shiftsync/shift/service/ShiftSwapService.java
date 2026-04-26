package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.shift.dto.ShiftSwapRequest;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapResponse;

public interface ShiftSwapService {

    ShiftSwapResponse requestSwap(Long actorUserId, ShiftSwapRequest request);

    void approveSwap(Long actorUserId, Long swapId);

    void rejectSwap(Long actorUserId, Long swapId, String managerNote);
}
