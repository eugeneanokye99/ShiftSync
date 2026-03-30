package com.shiftsync.shiftsync.department.service;

import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.department.dto.CreateDepartmentRequest;
import com.shiftsync.shiftsync.department.dto.DepartmentResponse;
import com.shiftsync.shiftsync.department.entity.Department;
import com.shiftsync.shiftsync.department.mapper.DepartmentMapper;
import com.shiftsync.shiftsync.department.repository.DepartmentRepository;
import com.shiftsync.shiftsync.department.service.impl.DepartmentServiceImpl;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Location location;
    private Department department;
    private DepartmentResponse response;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .id(1L)
                .name("Airport Branch")
                .address("Airport Road")
                .maxHeadcountPerShift(40)
                .active(true)
                .build();

        department = Department.builder()
                .id(10L)
                .name("Kitchen")
                .location(location)
                .build();

        response = new DepartmentResponse(10L, 1L, "Kitchen");
    }

    @Test
    void createDepartment_Success() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("Kitchen");
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(departmentRepository.existsByLocationIdAndNameIgnoreCase(1L, "Kitchen")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(departmentMapper.toResponse(department)).thenReturn(response);

        DepartmentResponse created = departmentService.createDepartment(1L, request);

        assertThat(created.id()).isEqualTo(10L);
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void createDepartment_LocationMissing_ThrowsNotFound() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.createDepartment(99L, new CreateDepartmentRequest("Kitchen")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Location not found");

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    void createDepartment_Duplicate_ThrowsConflict() {
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(departmentRepository.existsByLocationIdAndNameIgnoreCase(1L, "Kitchen")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(1L, new CreateDepartmentRequest("Kitchen")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Department name already exists for this location");
    }

    @Test
    void getDepartmentsByLocation_Success() {
        when(locationRepository.existsById(1L)).thenReturn(true);
        when(departmentRepository.findAllByLocationIdOrderByNameAsc(1L)).thenReturn(List.of(department));
        when(departmentMapper.toResponse(department)).thenReturn(response);

        List<DepartmentResponse> departments = departmentService.getDepartmentsByLocation(1L);

        assertThat(departments).hasSize(1);
        assertThat(departments.getFirst().name()).isEqualTo("Kitchen");
    }

    @Test
    void getDepartmentsByLocation_LocationMissing_ThrowsNotFound() {
        when(locationRepository.existsById(88L)).thenReturn(false);

        assertThatThrownBy(() -> departmentService.getDepartmentsByLocation(88L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Location not found");
    }
}

