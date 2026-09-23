package com.ridelink.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class FareEstimateRequest {
    @NotNull(message = "distanceKm is required")
    @Positive(message = "distanceKm must be positive")
    private Double distanceKm;

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
}
