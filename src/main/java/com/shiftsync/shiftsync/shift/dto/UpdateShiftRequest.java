package com.shiftsync.shiftsync.shift.dto;

import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateShiftRequest(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String requiredSkill,
        @Min(value = 1, message = "minimumHeadcount must be at least 1")
        Integer minimumHeadcount
) {}
