package com.shiftsync.shiftsync.location.service;


import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.employee.entity.Employee;
import com.shiftsync.shiftsync.employee.repository.EmployeeRepository;
import com.shiftsync.shiftsync.location.dto.CreateLocationRequest;
import com.shiftsync.shiftsync.location.dto.LocationResponse;
import com.shiftsync.shiftsync.location.dto.UpdateLocationRequest;
import com.shiftsync.shiftsync.location.entity.Location;
import com.shiftsync.shiftsync.location.entity.ManagerLocation;
import com.shiftsync.shiftsync.location.mapper.LocationMapper;
import com.shiftsync.shiftsync.location.repository.ManagerLocationRepository;
import com.shiftsync.shiftsync.location.repository.LocationRepository;
import com.shiftsync.shiftsync.location.service.impl.LocationServiceImpl;
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
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ManagerLocationRepository managerLocationRepository;

    @Mock
    private LocationMapper locationMapper;

    @InjectMocks
    private LocationServiceImpl locationService;

    private Location location;
    private LocationResponse response;
    private Employee managerEmployee;
    private User managerUser;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .id(1L)
                .name("Airport Branch")
                .address("Airport Road")
                .maxHeadcountPerShift(40)
                .active(true)
                .build();

        response = new LocationResponse(
                1L,
                "Airport Branch",
                "Airport Road",
                40,
                true
        );

        managerEmployee = Employee.builder()
                .id(20L)
                .user(managerUser)
                .build();

        managerUser = User.builder()
                .id(50L)
                .role(UserRole.MANAGER)
                .build();

        managerEmployee.setUser(managerUser);
    }

    @Test
    void createLocation_Success() {
        CreateLocationRequest request = new CreateLocationRequest("Airport Branch", "Airport Road", 40);
        when(locationRepository.existsByNameIgnoreCase("Airport Branch")).thenReturn(false);
        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(locationMapper.toResponse(location)).thenReturn(response);

        LocationResponse created = locationService.createLocation(request);

        assertThat(created.id()).isEqualTo(1L);
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void createLocation_DuplicateName_ThrowsConflict() {
        CreateLocationRequest request = new CreateLocationRequest("Airport Branch", "Airport Road", 40);
        when(locationRepository.existsByNameIgnoreCase("Airport Branch")).thenReturn(true);

        assertThatThrownBy(() -> locationService.createLocation(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Location name already exists");

        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void getActiveLocations_ReturnsMappedList() {
        when(locationRepository.findAllByActiveTrue()).thenReturn(List.of(location));
        when(locationMapper.toResponse(location)).thenReturn(response);

        List<LocationResponse> locations = locationService.getActiveLocations();

        assertThat(locations).hasSize(1);
        assertThat(locations.getFirst().name()).isEqualTo("Airport Branch");
    }

    @Test
    void updateLocation_Success() {
        UpdateLocationRequest request = new UpdateLocationRequest("East Legon Branch", "Boundary Road", 55, true);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(locationRepository.existsByNameIgnoreCaseAndIdNot("East Legon Branch", 1L)).thenReturn(false);
        when(locationRepository.save(location)).thenReturn(location);
        when(locationMapper.toResponse(location)).thenReturn(
                new LocationResponse(1L, "East Legon Branch", "Boundary Road", 55, true)
        );

        LocationResponse updated = locationService.updateLocation(1L, request);

        assertThat(updated.name()).isEqualTo("East Legon Branch");
        assertThat(location.getAddress()).isEqualTo("Boundary Road");
        assertThat(location.getMaxHeadcountPerShift()).isEqualTo(55);
    }

    @Test
    void updateLocation_NotFound_ThrowsNotFound() {
        when(locationRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.updateLocation(88L, new UpdateLocationRequest(null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Location not found");
    }

    @Test
    void updateLocation_DuplicateName_ThrowsConflict() {
        UpdateLocationRequest request = new UpdateLocationRequest("Airport Branch", null, null, null);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(locationRepository.existsByNameIgnoreCaseAndIdNot("Airport Branch", 1L)).thenReturn(true);

        assertThatThrownBy(() -> locationService.updateLocation(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Location name already exists");
    }

    @Test
    void getAssignedLocationsForManager_ReturnsEmptyListWhenNoAssignments() {
        when(employeeRepository.findByUserId(99L)).thenReturn(Optional.of(managerEmployee));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(20L)).thenReturn(List.of());

        List<LocationResponse> locations = locationService.getAssignedLocationsForManager(99L);

        assertThat(locations).isEmpty();
    }

    @Test
    void getAssignedLocationsForManager_ReturnsMappedLocations() {
        when(employeeRepository.findByUserId(99L)).thenReturn(Optional.of(managerEmployee));
        when(managerLocationRepository.findLocationIdsByManagerEmployeeId(20L)).thenReturn(List.of(1L));
        when(locationRepository.findAllById(List.of(1L))).thenReturn(List.of(location));
        when(locationMapper.toResponse(location)).thenReturn(response);

        List<LocationResponse> locations = locationService.getAssignedLocationsForManager(99L);

        assertThat(locations).hasSize(1);
        assertThat(locations.getFirst().name()).isEqualTo("Airport Branch");
    }

    @Test
    void getAssignedLocationsForManager_WhenManagerProfileMissing_ThrowsNotFound() {
        when(employeeRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getAssignedLocationsForManager(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Manager profile not found");
    }

    @Test
    void assignManagerToLocation_Success() {
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(managerEmployee));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(managerLocationRepository.existsByManagerIdAndLocationId(20L, 1L)).thenReturn(false);

        locationService.assignManagerToLocation(20L, 1L);

        verify(managerLocationRepository).save(any(ManagerLocation.class));
    }

    @Test
    void assignManagerToLocation_DuplicateAssignment_ThrowsConflict() {
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(managerEmployee));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(managerLocationRepository.existsByManagerIdAndLocationId(20L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> locationService.assignManagerToLocation(20L, 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Manager is already assigned to this location");
    }

    @Test
    void assignManagerToLocation_NotManager_ThrowsInvalidState() {
        managerUser.setRole(UserRole.EMPLOYEE);
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(managerEmployee));

        assertThatThrownBy(() -> locationService.assignManagerToLocation(20L, 1L))
                .isInstanceOf(InvalidStateException.class)
                .hasMessage("Employee is not a manager");
    }

    @Test
    void unassignManagerFromLocation_Success() {
        ManagerLocation mapping = ManagerLocation.builder()
                .id(5L)
                .manager(managerEmployee)
                .location(location)
                .build();
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(managerEmployee));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(managerLocationRepository.findByManagerIdAndLocationId(20L, 1L)).thenReturn(Optional.of(mapping));

        locationService.unassignManagerFromLocation(20L, 1L);

        verify(managerLocationRepository).delete(mapping);
    }

    @Test
    void unassignManagerFromLocation_MissingAssignment_ThrowsNotFound() {
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(managerEmployee));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(managerLocationRepository.findByManagerIdAndLocationId(20L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.unassignManagerFromLocation(20L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Manager assignment not found");
    }
}

