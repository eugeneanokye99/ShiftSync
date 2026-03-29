package com.shiftsync.shiftsync.employee.service.impl;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.department.entity.Department;
import com.shiftsync.shiftsync.department.repository.DepartmentRepository;
import com.shiftsync.shiftsync.employee.dto.CreateEmployeeRequest;
import com.shiftsync.shiftsync.employee.dto.EmployeePageResponse;
import com.shiftsync.shiftsync.employee.dto.EmployeeResponse;
import com.shiftsync.shiftsync.employee.dto.UpdateMyProfileRequest;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.mapper.EmployeeMapper;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.employee.service.EmployeeService;
import com.shiftsync.shiftsync.employee.specification.EmployeeSpecification;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final ManagerLocationRepository managerLocationRepository;
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
    public EmployeePageResponse getEmployees(
            Long actorUserId,
            boolean isManager,
            Long departmentId,
            Long locationId,
            EmploymentType employmentType,
            Boolean active,
            int page,
            int size,
            String sortBy
    ) {
        List<Long> scopedLocationIds = null;

        if (isManager) {
            Employee manager = employeeRepository.findByUserId(actorUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));
            List<Long> managerLocationIds = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());

            if (locationId != null && !managerLocationIds.contains(locationId)) {
                throw new AccessDeniedException("You can only view employees at your assigned locations");
            }

            scopedLocationIds = locationId != null ? List.of(locationId) : managerLocationIds;
        }

        Boolean activeFilter = active == null ? Boolean.TRUE : active;

        boolean useDefaultLastNameSort = sortBy == null || sortBy.isBlank() || "lastName".equalsIgnoreCase(sortBy);
        Pageable pageable = useDefaultLastNameSort
                ? PageRequest.of(page, size)
                : PageRequest.of(page, size, buildSort(sortBy));
        Page<Employee> employees = employeeRepository.findAll(
                EmployeeSpecification.withFilters(
                        departmentId,
                        locationId,
                        employmentType,
                        activeFilter,
                        scopedLocationIds,
                        useDefaultLastNameSort
                ),
                pageable
        );

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

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getMyProfile(Long actorUserId) {
        Employee employee = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse updateMyProfile(Long actorUserId, UpdateMyProfileRequest request) {
        if (request.employmentType() != null || request.departmentId() != null || request.locationId() != null) {
            throw new AccessDeniedException("You cannot update employment type, department, or location");
        }

        Employee employee = employeeRepository.findByUserId(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        if (request.phone() != null) {
            employee.setPhone(request.phone());
        }
        if (request.skills() != null) {
            employee.setSkills(request.skills().toArray(new String[0]));
        }
        if (request.notificationEnabled() != null) {
            employee.setNotificationEnabled(request.notificationEnabled());
        }

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long actorUserId, boolean isManager, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (isManager) {
            Employee manager = employeeRepository.findByUserId(actorUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager profile not found"));
            List<Long> managerLocationIds = managerLocationRepository.findLocationIdsByManagerEmployeeId(manager.getId());
            if (!managerLocationIds.contains(employee.getLocation().getId())) {
                throw new AccessDeniedException("You can only view employees at your assigned locations");
            }
        }

        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse deactivateEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (!Boolean.TRUE.equals(employee.getActive())) {
            throw new DuplicateResourceException("Employee is already inactive");
        }

        employee.setActive(false);
        employee.setDeactivatedAt(LocalDateTime.now());

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toResponse(saved);
    }

    private Sort buildSort(String sortBy) {
        String sortField = "hireDate".equalsIgnoreCase(sortBy) ? "hireDate" : "user.fullName";

        return Sort.by(Sort.Direction.ASC, sortField);
    }
}

