package com.ridelink.driver.controller;

import com.ridelink.driver.dto.AvailabilityRequest;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.dto.LocationRequest;
import com.ridelink.driver.exception.ApiException;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.repository.DriverRepository;
import com.ridelink.driver.security.AuthHelper;
import com.ridelink.driver.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/drivers")
@Tag(name = "Drivers", description = "Driver operational profile, vehicle, availability and location")
public class DriverController {

    private final DriverRepository driverRepository;

    public DriverController(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Operation(summary = "Register operational + vehicle profile (DRIVER only)")
    @PostMapping
    public Driver createProfile(@Valid @RequestBody CreateDriverRequest req, HttpServletRequest request) {
        AuthenticatedUser user = AuthHelper.requireAuth(request);
        AuthHelper.requireRole(user, "DRIVER");

        if (driverRepository.findByUserId(user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Driver profile already exists for this user");
        }

        Driver driver = new Driver(user.getId(), req.getVehiclePlate(), req.getVehicleModel(), req.getServiceArea());
        return driverRepository.save(driver);
    }

    @Operation(summary = "Get a driver profile")
    @GetMapping("/{id}")
    public Driver getDriver(@PathVariable String id, HttpServletRequest request) {
        AuthHelper.requireAuth(request);
        return driverRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    @Operation(summary = "Set ONLINE/OFFLINE availability (own profile only)")
    @PatchMapping("/{id}/availability")
    public Driver setAvailability(@PathVariable String id, @Valid @RequestBody AvailabilityRequest req,
                                   HttpServletRequest request) {
        AuthenticatedUser user = AuthHelper.requireAuth(request);
        AuthHelper.requireRole(user, "DRIVER");

        if (!List.of("ONLINE", "OFFLINE").contains(req.getAvailability())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "availability must be ONLINE or OFFLINE");
        }

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found"));
        if (!driver.getUserId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        driver.setAvailability(req.getAvailability());
        return driverRepository.save(driver);
    }

    @Operation(summary = "Update simulated current location (own profile only)")
    @PatchMapping("/{id}/location")
    public Driver setLocation(@PathVariable String id, @Valid @RequestBody LocationRequest req,
                               HttpServletRequest request) {
        AuthenticatedUser user = AuthHelper.requireAuth(request);
        AuthHelper.requireRole(user, "DRIVER");

        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Driver not found"));
        if (!driver.getUserId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        driver.setLat(req.getLat());
        driver.setLng(req.getLng());
        return driverRepository.save(driver);
    }

    @Operation(summary = "List drivers, optionally filtered by availability and service area " +
            "(called by ride-service to find an available driver)")
    @GetMapping
    public List<Driver> listDrivers(@RequestParam(required = false) String serviceArea,
                                     @RequestParam(required = false) Boolean available,
                                     HttpServletRequest request) {
        AuthHelper.requireAuth(request);

        List<Driver> drivers;
        if (Boolean.TRUE.equals(available) && serviceArea != null) {
            drivers = driverRepository.findByAvailabilityAndServiceArea("ONLINE", serviceArea);
        } else if (Boolean.TRUE.equals(available)) {
            drivers = driverRepository.findByAvailability("ONLINE");
        } else {
            drivers = driverRepository.findAll();
        }
        return drivers;
    }
}
