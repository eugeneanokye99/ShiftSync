package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.audit.annotation.Auditable;
import com.shiftsync.shiftsync.common.enums.AuditAction;
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
    @Auditable(entityType = "SHIFT_SWAP", action = AuditAction.CREATE)
    ShiftSwapResponse requestSwap(Long actorUserId, ShiftSwapRequest request);

    /**
     * Approve swap.
     *
     * @param actorUserId the actor user id
     * @param swapId      the swap id
     */
    @Auditable(entityType = "SHIFT_SWAP", action = AuditAction.UPDATE, entityIdParam = 1)
    void approveSwap(Long actorUserId, Long swapId);

    /**
     * Reject swap.
     *
     * @param actorUserId the actor user id
     * @param swapId      the swap id
     * @param managerNote the manager note
     */
    @Auditable(entityType = "SHIFT_SWAP", action = AuditAction.UPDATE, entityIdParam = 1)
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
