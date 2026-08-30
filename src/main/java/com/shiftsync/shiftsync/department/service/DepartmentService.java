package com.shiftsync.shiftsync.department.service;

import com.shiftsync.shiftsync.department.dto.CreateDepartmentRequest;
import com.shiftsync.shiftsync.department.dto.DepartmentResponse;

import java.util.List;

/**
 * The interface Department service.
 */
public interface DepartmentService {

    /**
     * Create department department response.
     *
     * @param locationId the location id
     * @param request    the request
     * @return the department response
     */
    DepartmentResponse createDepartment(Long locationId, CreateDepartmentRequest request);

    /**
     * Gets departments by location.
     *
     * @param locationId the location id
     * @return the departments by location
     */
    List<DepartmentResponse> getDepartmentsByLocation(Long locationId);
}

