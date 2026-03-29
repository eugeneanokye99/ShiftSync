package com.shiftsync.shiftsync.employee.mapper;

import com.shiftsync.shiftsync.employee.dto.EmployeeResponse;
import com.shiftsync.shiftsync.employee.entity.Employee;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getUser().getId(),
                employee.getUser().getFullName(),
                employee.getUser().getRole(),
                employee.getPhone(),
                employee.getEmploymentType(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getLocation().getId(),
                employee.getLocation().getName(),
                employee.getSkills() == null ? Collections.emptyList() : Arrays.asList(employee.getSkills()),
                employee.getNotificationEnabled(),
                employee.getContractedWeeklyHours(),
                employee.getHireDate(),
                employee.getActive()
        );
    }
}

