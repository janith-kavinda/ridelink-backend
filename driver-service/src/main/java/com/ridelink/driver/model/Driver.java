package com.ridelink.driver.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "drivers")
public class Driver {

    @Id
    private String id;

    private String userId; // references account-service User.id (no direct DB join across services)

    private String vehiclePlate;
    private String vehicleModel;
    private String serviceArea;

    private String availability = "OFFLINE"; // ONLINE, OFFLINE

    private Double lat;
    private Double lng; // simulated current location

    private Instant createdAt = Instant.now();

    public Driver() {
    }

    public Driver(String userId, String vehiclePlate, String vehicleModel, String serviceArea) {
        this.userId = userId;
        this.vehiclePlate = vehiclePlate;
        this.vehicleModel = vehicleModel;
        this.serviceArea = serviceArea;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
