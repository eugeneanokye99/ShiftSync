package com.shiftsync.shiftsync.employee.dto;

import java.util.List;

public record UpdateMyProfileRequest(
        String phone,
        List<String> skills,
        Boolean notificationEnabled
) {
}

