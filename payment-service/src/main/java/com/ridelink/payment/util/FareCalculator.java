package com.ridelink.payment.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Documented fare calculation rule (see README "Fare Rule" section):
 *   finalFare = BASE_FARE + (distanceKm * PER_KM_RATE)
 * Currency is a fictional "Rs" for simulation purposes only.
 */
@Component
public class FareCalculator {

    // Fixed base charge applied to every ride.
    private final double base;

    // Additional charge per kilometer traveled.
    private final double perKmRate;

    public FareCalculator(@Value("${fare.base}") double base,
                           @Value("${fare.per-km-rate}") double perKmRate) {
        this.base = base;
        this.perKmRate = perKmRate;
    }

    // Calculates the estimated or final fare for a trip using the configured pricing model.
    public double calculate(double distanceKm) {
        double fare = base + distanceKm * perKmRate;
        return Math.round(fare * 100.0) / 100.0;
    }
}
