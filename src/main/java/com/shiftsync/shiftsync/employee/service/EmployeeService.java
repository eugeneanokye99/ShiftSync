package com.shiftsync.shiftsync.employee.service;

import com.shiftsync.shiftsync.employee.dto.CreateEmployeeRequest;
import com.shiftsync.shiftsync.employee.dto.EmployeePageResponse;
import com.shiftsync.shiftsync.employee.dto.EmployeeResponse;
import com.shiftsync.shiftsync.employee.dto.GetEmployeesRequest;
import com.shiftsync.shiftsync.employee.dto.UpdateMyProfileRequest;

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
     * @param request the get employees request
     * @return the employees
     */
    EmployeePageResponse getEmployees(GetEmployeesRequest request);

    /**
     * Gets my profile.
     *
     * @param actorUserId the actor user id
     * @return the my profile
     */
    EmployeeResponse getMyProfile(Long actorUserId);

    /**
     * Update my profile employee response.
     *
     * @param actorUserId the actor user id
     * @param request     the request
     * @return the employee response
     */
    EmployeeResponse updateMyProfile(Long actorUserId, UpdateMyProfileRequest request);

    /**
     * Gets employee by id.
     *
     * @param actorUserId the actor user id
     * @param isManager   the is manager
     * @param employeeId  the employee id
     * @return the employee by id
     */
    EmployeeResponse getEmployeeById(Long actorUserId, boolean isManager, Long employeeId);

    /**
     * Deactivate employee employee response.
     *
     * @param employeeId the employee id
     * @return the employee response
     */
    EmployeeResponse deactivateEmployee(Long employeeId);
}

