package com.ridelink.driver.controller;

import com.ridelink.driver.dto.AvailabilityRequest;
import com.ridelink.driver.dto.CreateDriverRequest;
import com.ridelink.driver.exception.ApiException;
import com.ridelink.driver.model.Driver;
import com.ridelink.driver.repository.DriverRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private DriverController driverController;

    private void mockAuthUser(String userId, String role) {
        var user = new com.ridelink.driver.security.AuthenticatedUser(userId, role, "x@example.com");
        when(httpRequest.getAttribute("authUser")).thenReturn(user);
    }

    @Test
    void driverCanCreateProfileAndGoOnline() {
        mockAuthUser("driver-user-1", "DRIVER");

        CreateDriverRequest createReq = new CreateDriverRequest();
        createReq.setVehiclePlate("WP-CAB-1234");
        createReq.setVehicleModel("Toyota Prius");
        createReq.setServiceArea("Colombo");

        when(driverRepository.findByUserId("driver-user-1")).thenReturn(Optional.empty());
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> {
            Driver d = inv.getArgument(0);
            d.setId("driver-1");
            return d;
        });

        Driver created = driverController.createProfile(createReq, httpRequest);
        assertEquals("Colombo", created.getServiceArea());
        assertEquals("OFFLINE", created.getAvailability());

        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(created));
        AvailabilityRequest availReq = new AvailabilityRequest();
        availReq.setAvailability("ONLINE");

        Driver updated = driverController.setAvailability("driver-1", availReq, httpRequest);
        assertEquals("ONLINE", updated.getAvailability());
    }

    @Test
    void rejectsAvailabilityUpdateForSomeoneElsesDriverProfile() {
        mockAuthUser("intruder-user", "DRIVER");

        Driver otherDriversProfile = new Driver("owner-user", "WP-1", "Corolla", "Kandy");
        otherDriversProfile.setId("driver-2");
        when(driverRepository.findById("driver-2")).thenReturn(Optional.of(otherDriversProfile));

        AvailabilityRequest availReq = new AvailabilityRequest();
        availReq.setAvailability("ONLINE");

        ApiException ex = assertThrows(ApiException.class,
                () -> driverController.setAvailability("driver-2", availReq, httpRequest));
        assertEquals(403, ex.getStatus().value());
    }
}
