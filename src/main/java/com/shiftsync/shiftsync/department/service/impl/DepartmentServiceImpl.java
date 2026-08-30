package com.shiftsync.shiftsync.department.service.impl;

import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.department.dto.CreateDepartmentRequest;
import com.shiftsync.shiftsync.department.dto.DepartmentResponse;
import com.shiftsync.shiftsync.department.entity.Department;
import com.shiftsync.shiftsync.department.mapper.DepartmentMapper;
import com.shiftsync.shiftsync.department.repository.DepartmentRepository;
import com.shiftsync.shiftsync.department.service.DepartmentService;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(Long locationId, CreateDepartmentRequest request) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));

        String normalizedName = request.name().trim();
        if (departmentRepository.existsByLocationIdAndNameIgnoreCase(locationId, normalizedName)) {
            throw new DuplicateResourceException("Department name already exists for this location");
        }

        Department department = Department.builder()
                .location(location)
                .name(normalizedName)
                .build();

        Department saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentsByLocation(Long locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found");
        }

        return departmentRepository.findAllByLocationIdOrderByNameAsc(locationId)
                .stream()
                .map(departmentMapper::toResponse)
                .toList();
    }
}

