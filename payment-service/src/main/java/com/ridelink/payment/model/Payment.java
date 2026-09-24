package com.ridelink.payment.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payments")
public class Payment {

    // Represents a completed or failed payment record for a ride transaction.
    @Id
    private String id; // MongoDB document id

    private String rideId; // references ride-service Ride.id (no direct DB join)
    private String passengerId; // passenger who paid for the ride

    private double distanceKm; // distance used to calculate fare
    private double fare; // final computed fare for this ride
    private String currency = "Rs"; // currency used for this payment

    private String status; // PAID, FAILED
    private String receiptNumber; // unique external receipt identifier

    private Instant createdAt = Instant.now(); // payment creation timestamp

    public Payment() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
