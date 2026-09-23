package com.ridelink.ride.controller;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.PaymentServiceClient;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.UpdateStatusRequest;
import com.ridelink.ride.exception.ApiException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.security.AuthHelper;
import com.ridelink.ride.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rides")
@Tag(name = "Rides", description = "Ride requests, driver assignment and the ride status lifecycle")
public class RideController {

    // Valid ride lifecycle transitions
    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            "REQUESTED", List.of("ASSIGNED", "CANCELLED"),
            "ASSIGNED", List.of("ACCEPTED", "CANCELLED"),
            "ACCEPTED", List.of("IN_PROGRESS", "CANCELLED"),
            "IN_PROGRESS", List.of("COMPLETED", "CANCELLED"),
            "COMPLETED", List.of(),
            "CANCELLED", List.of()
    );

    private final RideRepository rideRepository;
    private final DriverServiceClient driverServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    public RideController(RideRepository rideRepository,
                           DriverServiceClient driverServiceClient,
                           PaymentServiceClient paymentServiceClient) {
        this.rideRepository = rideRepository;
        this.driverServiceClient = driverServiceClient;
        this.paymentServiceClient = paymentServiceClient;
    }

    @Operation(summary = "Create a ride request (PASSENGER only)")
    @PostMapping
    public Ride createRide(@Valid @RequestBody CreateRideRequest req, HttpServletRequest request) {
        AuthenticatedUser user = AuthHelper.requireAuth(request);
        AuthHelper.requireRole(user, "PASSENGER");

        Ride ride = new Ride();
        ride.setPassengerId(user.getId());
        ride.setPickup(req.getPickup());
        ride.setDestination(req.getDestination());
        ride.setServiceArea(req.getServiceArea());
        ride.setStatus("REQUESTED");
        ride.addHistory("REQUESTED");

        return rideRepository.save(ride);
    }

    @Operation(summary = "Assign an available driver to a REQUESTED ride (interservice call to driver-service)")
    @PostMapping("/{id}/assign")
    public Ride assignDriver(@PathVariable String id, HttpServletRequest request) {
        AuthenticatedUser user = AuthHelper.requireAuth(request);
        String token = AuthHelper.requireToken(request);

        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ride not found"));

        if (!"REQUESTED".equals(ride.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cannot assign a ride in status " + ride.getStatus());
        }

        Map<String, Object> driver;
        try {
            driver = driverServiceClient.findAvailableDriver(ride.getServiceArea(), token);
        } catch (DriverServiceClient.DriverServiceUnavailableException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Driver service unavailable");
        }

        if (driver == null) {
            // Negative scenario: no available driver
            throw new ApiException(HttpStatus.CONFLICT, "No available driver in this service area");
        }

        ride.setDriverId(String.valueOf(driver.get("id")));
        ride.setStatus("ASSIGNED");
        ride.addHistory("ASSIGNED");
        return rideRepository.save(ride);
    }

    @Operation(summary = "Update ride status; COMPLETED triggers a call to payment-service")
    @PatchMapping("/{id}/status")
    public Ride updateStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest req,
                              HttpServletRequest request) {
        AuthenticatedUser user = AuthHelper.requireAuth(request);
        String token = AuthHelper.requireToken(request);

        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ride not found"));

        List<String> allowed = TRANSITIONS.getOrDefault(ride.getStatus(), List.of());
        if (!allowed.contains(req.getStatus())) {
            // Negative scenario: invalid status transition
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid transition from " + ride.getStatus() + " to " + req.getStatus()
                            + " (allowed: " + allowed + ")");
        }

        ride.setStatus(req.getStatus());
        ride.addHistory(req.getStatus());

        if ("COMPLETED".equals(req.getStatus())) {
            // Interservice call #2: ask payment-service to record the fare & payment.
            // distanceKm is a placeholder here - a full build should compute this with the
            // Haversine formula from ride.getPickup()/ride.getDestination().
            double distanceKm = 5.2;
            try {
                Map<String, Object> payment = paymentServiceClient.recordPayment(
                        ride.getId(), ride.getPassengerId(), distanceKm, token);
                ride.setPaymentId(String.valueOf(payment.get("id")));
            } catch (PaymentServiceClient.PaymentServiceUnavailableException ex) {
                ride.setPaymentError("Payment service unavailable at completion time");
            }
        }

        return rideRepository.save(ride);
    }

    @Operation(summary = "Get a single ride (passenger, assigned driver, or ADMIN)")
    @GetMapping("/{id}")
    public Ride getRide(@PathVariable String id, HttpServletRequest request) {
        AuthenticatedUser user = AuthHelper.requireAuth(request);
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ride not found"));

        boolean owns = ride.getPassengerId().equals(user.getId())
                || user.getId().equals(ride.getDriverId())
                || "ADMIN".equals(user.getRole());
        if (!owns) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        return ride;
    }

    @Operation(summary = "List the current user's rides (as passenger or driver)")
    @GetMapping
    public List<Ride> listRides(HttpServletRequest request) {
        AuthenticatedUser user = AuthHelper.requireAuth(request);
        return rideRepository.findByPassengerIdOrDriverId(user.getId(), user.getId());
    }
}
