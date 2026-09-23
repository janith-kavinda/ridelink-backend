package com.ridelink.payment.controller;

import com.ridelink.payment.dto.CreatePaymentRequest;
import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.exception.ApiException;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.security.AuthenticatedUser;
import com.ridelink.payment.util.FareCalculator;
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
class PaymentControllerTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private HttpServletRequest httpRequest;

    private final FareCalculator fareCalculator = new FareCalculator(100, 50);

    private PaymentController controller() {
        return new PaymentController(paymentRepository, fareCalculator);
    }

    private void mockAuthUser() {
        var user = new AuthenticatedUser("passenger-1", "PASSENGER", "x@example.com");
        when(httpRequest.getAttribute("authUser")).thenReturn(user);
    }

    @Test
    void estimatesFareUsingDocumentedRule() {
        mockAuthUser();
        FareEstimateRequest req = new FareEstimateRequest();
        req.setDistanceKm(10.0);

        var response = controller().estimateFare(req, httpRequest);

        assertEquals(600.0, (Double) response.get("estimatedFare")); // 100 + 10*50
    }

    @Test
    void recordsPaymentSuccessfully() {
        mockAuthUser();
        when(paymentRepository.findByRideId("ride-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId("payment-1");
            return p;
        });

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setRideId("ride-1");
        req.setPassengerId("passenger-1");
        req.setDistanceKm(5.0);

        var response = controller().createPayment(req, httpRequest);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("PAID", response.getBody().getStatus());
        assertEquals(350.0, response.getBody().getFare()); // 100 + 5*50
    }

    @Test
    void rejectsDuplicatePaymentForSameRide() {
        mockAuthUser();
        Payment existing = new Payment();
        existing.setId("payment-1");
        existing.setRideId("ride-1");
        when(paymentRepository.findByRideId("ride-1")).thenReturn(Optional.of(existing));

        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setRideId("ride-1");
        req.setPassengerId("passenger-1");
        req.setDistanceKm(5.0);

        ApiException ex = assertThrows(ApiException.class, () -> controller().createPayment(req, httpRequest));
        assertEquals(409, ex.getStatus().value());
    }
}
