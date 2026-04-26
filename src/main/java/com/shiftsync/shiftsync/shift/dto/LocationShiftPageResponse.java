package com.shiftsync.shiftsync.shift.dto;

import java.util.List;

public record LocationShiftPageResponse(
        List<LocationShiftResponse> content,
        int totalElements,
        int totalPages,
        int currentPage
) {
}
