package com.ridelink.driver.dto;

import jakarta.validation.constraints.NotBlank;

public class AvailabilityRequest {
    @NotBlank
    private String availability; // ONLINE / OFFLINE

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
}
