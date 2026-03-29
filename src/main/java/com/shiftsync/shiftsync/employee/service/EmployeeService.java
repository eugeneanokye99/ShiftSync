package com.shiftsync.shiftsync.employee.service;

import com.shiftsync.shiftsync.employee.dto.CreateEmployeeRequest;
import com.shiftsync.shiftsync.employee.dto.EmployeePageResponse;
import com.shiftsync.shiftsync.employee.dto.EmployeeResponse;

/**
 * The interface Employee service.
 */
public interface EmployeeService {

    /**
     * Create employee employee response.
     *
     * @param request the request
     * @return the employee response
     */
    EmployeeResponse createEmployee(CreateEmployeeRequest request);

    /**
     * Gets employees.
     *
     * @param page      the page
     * @param size      the size
     * @param sortBy    the sort by
     * @param direction the direction
     * @return the employees
     */
    EmployeePageResponse getEmployees(int page, int size, String sortBy, String direction);
}

