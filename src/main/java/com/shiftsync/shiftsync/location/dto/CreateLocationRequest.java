package com.shiftsync.shiftsync.location.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLocationRequest(
        @NotBlank(message = "Location name is required")
        String name,

        @NotBlank(message = "Location address is required")
        String address,

        @NotNull(message = "Max headcount per shift is required")
        @Min(value = 1, message = "Max headcount per shift must be at least 1")
        Integer maxHeadcountPerShift
) {
}

