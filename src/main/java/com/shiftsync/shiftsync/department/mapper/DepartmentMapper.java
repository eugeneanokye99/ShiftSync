package com.shiftsync.shiftsync.department.mapper;

import com.shiftsync.shiftsync.department.dto.DepartmentResponse;
import com.shiftsync.shiftsync.department.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getLocation().getId(),
                department.getName()
        );
    }
}

