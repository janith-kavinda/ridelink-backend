package com.ridelink.payment.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ridelink.payment.dto.CreatePaymentRequest;
import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.exception.ApiException;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.security.AuthHelper;
import com.ridelink.payment.util.FareCalculator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@Tag(
    name = "Fares & Payments",
    description = "Fare estimation, final fare calculation, simulated payments and receipts"
)
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final FareCalculator fareCalculator;

    public PaymentController(PaymentRepository paymentRepository, FareCalculator fareCalculator) {
        this.paymentRepository = paymentRepository;
        this.fareCalculator = fareCalculator;
    }

    // Calculates a fare estimate without creating a payment record.
    @Operation(summary = "Estimate a fare for a given distance")
    @PostMapping("/fares/estimate")
    public Map<String, Object> estimateFare(@Valid @RequestBody FareEstimateRequest req,
                                            HttpServletRequest request) {
        AuthHelper.requireAuth(request);

        double estimate = fareCalculator.calculate(req.getDistanceKm());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("distanceKm", req.getDistanceKm());
        body.put("estimatedFare", estimate);
        body.put("currency", "Rs");
        return body;
    }

    // Calculates and stores the final payment for a completed ride.
    @Operation(summary = "Record a simulated payment for a completed ride " +
            "(called by ride-service on ride completion)")
    @PostMapping("/payments")
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody CreatePaymentRequest req,
                                                 HttpServletRequest request) {
        AuthHelper.requireAuth(request);

        if (paymentRepository.findByRideId(req.getRideId()).isPresent()) {
            // Negative scenario: duplicate payment for the same ride
            throw new ApiException(HttpStatus.CONFLICT, "Payment already recorded for this ride");
        }

        double fare = fareCalculator.calculate(req.getDistanceKm());

        Payment payment = new Payment();
        payment.setRideId(req.getRideId());
        payment.setPassengerId(req.getPassengerId());
        payment.setDistanceKm(req.getDistanceKm());
        payment.setFare(fare);
        payment.setReceiptNumber("RCPT-" + System.currentTimeMillis());

        // Simulated payment - fails only in the degenerate case of zero/negative distance,
        // giving a concrete "failed simulated payment" negative scenario for the demo.
        boolean success = req.getDistanceKm() > 0;
        payment.setStatus(success ? "PAID" : "FAILED");

        payment = paymentRepository.save(payment);

        if (!success) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(payment);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    // Returns one payment record, or a 404 response when the id is unknown.
    @Operation(summary = "Retrieve a payment / receipt by id")
    @GetMapping("/payments/{id}")
    public Payment getPayment(@PathVariable String id, HttpServletRequest request) {
        AuthHelper.requireAuth(request);
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    // Lists all payments, or only the payments belonging to the requested ride.
    @Operation(summary = "List payments, optionally filtered by rideId")
    @GetMapping("/payments")
    public List<Payment> listPayments(@RequestParam(required = false) String rideId,
                                      HttpServletRequest request) {
        AuthHelper.requireAuth(request);
        if (rideId != null) {
            return paymentRepository.findAllByRideId(rideId);
        }
        return paymentRepository.findAll();
    }
}