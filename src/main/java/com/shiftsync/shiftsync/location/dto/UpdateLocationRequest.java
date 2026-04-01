package com.shiftsync.shiftsync.location.dto;

import jakarta.validation.constraints.Min;

public record UpdateLocationRequest(
        String name,
        String address,
        @Min(value = 1, message = "Max headcount per shift must be at least 1")
        Integer maxHeadcountPerShift,
        Boolean active
) {
}

