package com.ridelink.ride.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "rides")
public class Ride {

    @Id
    private String id;

    private String passengerId;
    private String driverId;

    private GeoPoint pickup;
    private GeoPoint destination;
    private String serviceArea;

    private String status = "REQUESTED";
    // REQUESTED -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
    //                                   \-> CANCELLED (from any non-terminal state)

    private String paymentId;
    private String paymentError;

    private Instant createdAt = Instant.now();
    private List<StatusEvent> history = new ArrayList<>();

    public Ride() {
    }

    public static class GeoPoint {
        private Double lat;
        private Double lng;
        private String label;

        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        public Double getLng() { return lng; }
        public void setLng(Double lng) { this.lng = lng; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class StatusEvent {
        private String status;
        private Instant at;

        public StatusEvent() { }
        public StatusEvent(String status, Instant at) {
            this.status = status;
            this.at = at;
        }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getAt() { return at; }
        public void setAt(Instant at) { this.at = at; }
    }

    public void addHistory(String status) {
        this.history.add(new StatusEvent(status, Instant.now()));
    }

    // Getters and setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    public GeoPoint getPickup() { return pickup; }
    public void setPickup(GeoPoint pickup) { this.pickup = pickup; }
    public GeoPoint getDestination() { return destination; }
    public void setDestination(GeoPoint destination) { this.destination = destination; }
    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getPaymentError() { return paymentError; }
    public void setPaymentError(String paymentError) { this.paymentError = paymentError; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<StatusEvent> getHistory() { return history; }
    public void setHistory(List<StatusEvent> history) { this.history = history; }
}
