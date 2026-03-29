package com.shiftsync.shiftsync.employee.service;

import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.enums.UserRole;
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
import com.shiftsync.shiftsync.employee.service.impl.EmployeeServiceImpl;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.location.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ManagerLocationRepository managerLocationRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private User user;
    private Department department;
    private Location location;
    private Employee employee;
    private CreateEmployeeRequest request;
    private EmployeeResponse response;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("jane@shiftsync.com")
                .fullName("Jane Doe")
                .passwordHash("hash")
                .role(UserRole.EMPLOYEE)
                .build();

        department = Department.builder()
                .id(2L)
                .name("Kitchen")
                .build();

        location = Location.builder()
                .id(3L)
                .name("Airport Branch")
                .build();

        employee = Employee.builder()
                .id(10L)
                .user(user)
                .department(department)
                .location(location)
                .phone("+233000000000")
                .employmentType(EmploymentType.FULL_TIME)
                .skills(new String[]{"barista"})
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .hireDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();

        request = new CreateEmployeeRequest(
                1L,
                "+233000000000",
                EmploymentType.FULL_TIME,
                2L,
                3L,
                List.of("barista"),
                new BigDecimal("40.00"),
                LocalDate.of(2026, 1, 1)
        );

        response = new EmployeeResponse(
                10L,
                1L,
                "Jane Doe",
                "jane@shiftsync.com",
                UserRole.EMPLOYEE,
                "+233000000000",
                EmploymentType.FULL_TIME,
                2L,
                "Kitchen",
                3L,
                "Airport Branch",
                List.of("barista"),
                true,
                new BigDecimal("40.00"),
                LocalDate.of(2026, 1, 1),
                true
        );
    }

    @Test
    void createEmployee_Success() {
        when(employeeRepository.existsByUserId(request.userId())).thenReturn(false);
        when(userRepository.findById(request.userId())).thenReturn(Optional.of(user));
        when(departmentRepository.findById(request.departmentId())).thenReturn(Optional.of(department));
        when(locationRepository.findById(request.locationId())).thenReturn(Optional.of(location));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse created = employeeService.createEmployee(request);

        assertThat(created.employeeId()).isEqualTo(10L);
        assertThat(created.fullName()).isEqualTo("Jane Doe");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void createEmployee_ProfileAlreadyExists_ThrowsDuplicateResourceException() {
        when(employeeRepository.existsByUserId(request.userId())).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Employee profile already exists for this user");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void createEmployee_UserNotFound_ThrowsResourceNotFoundException() {
        when(employeeRepository.existsByUserId(request.userId())).thenReturn(false);
        when(userRepository.findById(request.userId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void getEmployees_ReturnsPageResponse() {
        Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(0, 10), 1);
        when(employeeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class))).thenReturn(page);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeePageResponse result = employeeService.getEmployees(
                1L,
                false,
                null,
                null,
                null,
                true,
                0,
                10,
                "name"
        );

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.currentPage()).isEqualTo(0);
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void updateMyProfile_RestrictedFields_ThrowsAccessDeniedException() {
        UpdateMyProfileRequest updateRequest = new UpdateMyProfileRequest(
                null,
                null,
                null,
                EmploymentType.CONTRACT,
                null,
                null
        );

        assertThatThrownBy(() -> employeeService.updateMyProfile(1L, updateRequest))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You cannot update employment type, department, or location");
    }

    @Test
    void deactivateEmployee_AlreadyInactive_ThrowsDuplicateResourceException() {
        employee.setActive(false);
        employee.setDeactivatedAt(LocalDateTime.now());
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> employeeService.deactivateEmployee(10L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Employee is already inactive");
    }

    @Test
    void getEmployeeById_ManagerOutsideAssignedLocation_ThrowsAccessDeniedException() {
        User managerUser = User.builder()
                .id(99L)
                .email("manager@shiftsync.com")
                .fullName("Manager One")
                .passwordHash("hash")
                .role(UserRole.MANAGER)
                .build();
        Employee managerEmployee = Employee.builder()
                .id(77L)
                .user(managerUser)
                .department(department)
                .location(location)
                .employmentType(EmploymentType.FULL_TIME)
                .contractedWeeklyHours(new BigDecimal("40.00"))
                .active(true)
                .build();

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.findByUserId(99L)).thenReturn(Optional.of(managerEmployee));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(77L)).thenReturn(List.of(99L));

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L, true, 10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You can only view employees at your assigned locations");
    }
}


