package com.ridelink.ride.controller;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.PaymentServiceClient;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.UpdateStatusRequest;
import com.ridelink.ride.exception.ApiException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideControllerTest {

    @Mock
    private RideRepository rideRepository;
    @Mock
    private DriverServiceClient driverServiceClient;
    @Mock
    private PaymentServiceClient paymentServiceClient;
    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private RideController rideController;

    private void mockAuthUser(String userId, String role) {
        var user = new AuthenticatedUser(userId, role, "x@example.com");
        when(httpRequest.getAttribute("authUser")).thenReturn(user);
        when(httpRequest.getAttribute("authToken")).thenReturn("fake-token");
    }

    private Ride.GeoPoint point(double lat, double lng, String label) {
        Ride.GeoPoint p = new Ride.GeoPoint();
        p.setLat(lat);
        p.setLng(lng);
        p.setLabel(label);
        return p;
    }

    @Test
    void createsRideAndAssignsAvailableDriver() {
        mockAuthUser("passenger-1", "PASSENGER");

        CreateRideRequest createReq = new CreateRideRequest();
        createReq.setPickup(point(6.9, 79.8, "Fort"));
        createReq.setDestination(point(6.93, 79.85, "Rajagiriya"));
        createReq.setServiceArea("Colombo");

        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            if (r.getId() == null) r.setId("ride-1");
            return r;
        });

        Ride created = rideController.createRide(createReq, httpRequest);
        assertEquals("REQUESTED", created.getStatus());

        when(rideRepository.findById("ride-1")).thenReturn(Optional.of(created));
        when(driverServiceClient.findAvailableDriver(eq("Colombo"), anyString()))
                .thenReturn(Map.of("id", "driver-1"));

        Ride assigned = rideController.assignDriver("ride-1", httpRequest);
        assertEquals("ASSIGNED", assigned.getStatus());
        assertEquals("driver-1", assigned.getDriverId());
    }

    @Test
    void returns409WhenNoDriverAvailable() {
        mockAuthUser("passenger-2", "PASSENGER");

        Ride ride = new Ride();
        ride.setId("ride-2");
        ride.setPassengerId("passenger-2");
        ride.setServiceArea("Kandy");
        ride.setStatus("REQUESTED");

        when(rideRepository.findById("ride-2")).thenReturn(Optional.of(ride));
        when(driverServiceClient.findAvailableDriver(eq("Kandy"), anyString())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> rideController.assignDriver("ride-2", httpRequest));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void rejectsInvalidStatusTransition() {
        mockAuthUser("passenger-3", "PASSENGER");

        Ride ride = new Ride();
        ride.setId("ride-3");
        ride.setPassengerId("passenger-3");
        ride.setStatus("REQUESTED"); // jumping straight to COMPLETED is invalid

        when(rideRepository.findById("ride-3")).thenReturn(Optional.of(ride));

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus("COMPLETED");

        ApiException ex = assertThrows(ApiException.class,
                () -> rideController.updateStatus("ride-3", req, httpRequest));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void completingARideTriggersPaymentServiceCall() {
        mockAuthUser("passenger-4", "PASSENGER");

        Ride ride = new Ride();
        ride.setId("ride-4");
        ride.setPassengerId("passenger-4");
        ride.setStatus("IN_PROGRESS");

        when(rideRepository.findById("ride-4")).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentServiceClient.recordPayment(eq("ride-4"), eq("passenger-4"), anyDouble(), anyString()))
                .thenReturn(Map.of("id", "payment-1"));

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus("COMPLETED");

        Ride completed = rideController.updateStatus("ride-4", req, httpRequest);
        assertEquals("COMPLETED", completed.getStatus());
        assertEquals("payment-1", completed.getPaymentId());
        verify(paymentServiceClient).recordPayment(eq("ride-4"), eq("passenger-4"), anyDouble(), anyString());
    }
}
