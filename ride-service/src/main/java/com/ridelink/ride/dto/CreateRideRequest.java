package com.ridelink.ride.dto;

import com.ridelink.ride.model.Ride;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateRideRequest {
    @NotNull(message = "pickup is required")
    @Valid
    private Ride.GeoPoint pickup;

    @NotNull(message = "destination is required")
    @Valid
    private Ride.GeoPoint destination;

    @NotBlank(message = "serviceArea is required")
    private String serviceArea;

    public Ride.GeoPoint getPickup() { return pickup; }
    public void setPickup(Ride.GeoPoint pickup) { this.pickup = pickup; }
    public Ride.GeoPoint getDestination() { return destination; }
    public void setDestination(Ride.GeoPoint destination) { this.destination = destination; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
}
