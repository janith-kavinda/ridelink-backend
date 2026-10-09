package com.ridelink.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ridelink.payment.model.PaymentMethod;

public class CreatePaymentRequest {
    @NotBlank(message = "rideId is required")
    private String rideId;

    @NotBlank(message = "passengerId is required")
    private String passengerId;

    @NotNull(message = "distanceKm is required")
    private Double distanceKm;

    // CASH keeps ride-service's existing completion callback backwards compatible.
    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
