package com.shiftsync.shiftsync.shift.service;

import com.shiftsync.shiftsync.shift.dto.CreateShiftRequest;
import com.shiftsync.shiftsync.shift.dto.EmployeeShiftResponse;
import com.shiftsync.shiftsync.shift.dto.LocationShiftPageResponse;
import com.shiftsync.shiftsync.shift.dto.ShiftResponse;

import java.time.LocalDate;
import java.util.List;

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

    /**
     * Returns the authenticated employee's assigned shifts within the given date range.
     *
     * @param actorUserId      the employee's user ID
     * @param from             start of the date range (inclusive)
     * @param to               end of the date range (inclusive)
     * @param includeCancelled whether to include cancelled shifts
     * @return list of shift responses with colleague info
     */
    List<EmployeeShiftResponse> getMyShifts(Long actorUserId, LocalDate from, LocalDate to, boolean includeCancelled);

    /**
     * Returns a paginated shift schedule for a location, with staffing status per shift.
     *
     * @param actorUserId  the authenticated user's ID
     * @param locationId   the location to query
     * @param from         start of the date range (inclusive)
     * @param to           end of the date range (inclusive)
     * @param departmentId optional department filter
     * @param page         zero-based page number
     * @param size         page size
     * @return paginated location shift schedule
     */
    LocationShiftPageResponse getLocationShifts(Long actorUserId, Long locationId, LocalDate from, LocalDate to, Long departmentId, int page, int size);
}
