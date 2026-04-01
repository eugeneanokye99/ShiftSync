package com.shiftsync.shiftsync.location.dto;

public record LocationResponse(
        Long id,
        String name,
        String address,
        Integer maxHeadcountPerShift,
        Boolean active
) {
}

