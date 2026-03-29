package com.shiftsync.shiftsync.employee.service.impl;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.department.entity.Department;
import com.shiftsync.shiftsync.department.repository.DepartmentRepository;
import com.shiftsync.shiftsync.employee.dto.CreateEmployeeRequest;
import com.shiftsync.shiftsync.employee.dto.EmployeePageResponse;
import com.shiftsync.shiftsync.employee.dto.EmployeeResponse;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.mapper.EmployeeMapper;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.employee.service.EmployeeService;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByUserId(request.userId())) {
            throw new DuplicateResourceException("Employee profile already exists for this user");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        Location location = locationRepository.findById(request.locationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));

        Employee employee = Employee.builder()
                .user(user)
                .phone(request.phone())
                .employmentType(request.employmentType())
                .department(department)
                .location(location)
                .skills(request.skills() == null ? null : request.skills().toArray(new String[0]))
                .contractedWeeklyHours(request.contractedWeeklyHours())
                .hireDate(request.hireDate())
                .build();

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeePageResponse getEmployees(int page, int size, String sortBy, String direction) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, direction));
        Page<Employee> employees = employeeRepository.findAll(pageable);

        List<EmployeeResponse> content = employees.getContent()
                .stream()
                .map(employeeMapper::toResponse)
                .toList();

        return new EmployeePageResponse(
                content,
                employees.getTotalElements(),
                employees.getTotalPages(),
                employees.getNumber()
        );
    }

    private Sort buildSort(String sortBy, String direction) {
        String sortField = "name".equalsIgnoreCase(sortBy) ? "user.fullName"
                : "hireDate".equalsIgnoreCase(sortBy) ? "hireDate"
                : "user.fullName";

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(sortDirection, sortField);
    }
}

