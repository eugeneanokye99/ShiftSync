package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.shift.dto.ShiftSwapRequest;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapResponse;
import com.shiftsync.shiftsync.shift.entity.ShiftSwapStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The interface Shift swap service.
 */
public interface ShiftSwapService {

    /**
     * Request swap shift swap response.
     *
     * @param actorUserId the actor user id
     * @param request     the request
     * @return the shift swap response
     */
    ShiftSwapResponse requestSwap(Long actorUserId, ShiftSwapRequest request);

    /**
     * Approve swap.
     *
     * @param actorUserId the actor user id
     * @param swapId      the swap id
     */
    void approveSwap(Long actorUserId, Long swapId);

    /**
     * Reject swap.
     *
     * @param actorUserId the actor user id
     * @param swapId      the swap id
     * @param managerNote the manager note
     */
    void rejectSwap(Long actorUserId, Long swapId, String managerNote);

    /**
     * Gets my swaps.
     *
     * @param actorUserId the actor user id
     * @param status      the status
     * @return the my swaps
     */
    Page<ShiftSwapResponse> getMySwaps(Long actorUserId, ShiftSwapStatus status, Pageable pageable);
}
