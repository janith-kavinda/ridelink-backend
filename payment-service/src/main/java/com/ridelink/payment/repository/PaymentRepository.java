package com.ridelink.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ridelink.payment.model.Payment;

// Repository for payment records stored in MongoDB.
public interface PaymentRepository extends MongoRepository<Payment, String> {
    // Returns the payment for a specific ride, if one already exists.
    Optional<Payment> findByRideId(String rideId);

    // Returns all payment entries linked to a ride.
    List<Payment> findAllByRideId(String rideId);
}
