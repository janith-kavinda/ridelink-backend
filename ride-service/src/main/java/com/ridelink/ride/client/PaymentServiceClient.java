package com.ridelink.ride.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Interservice communication #2: ride-service -> payment-service (synchronous REST).
 * Triggered when a ride transitions to COMPLETED, to record the final fare and a
 * simulated payment.
 */
@Component
public class PaymentServiceClient {

    private final RestTemplate restTemplate;
    private final String paymentServiceUrl;

    public PaymentServiceClient(RestTemplate restTemplate,
                                 @Value("${services.payment-service.url}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> recordPayment(String rideId, String passengerId, double distanceKm,
                                              String bearerToken) {
        String url = paymentServiceUrl + "/payments";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + bearerToken);

        Map<String, Object> body = Map.of(
                "rideId", rideId,
                "passengerId", passengerId,
                "distanceKm", distanceKm
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return response.getBody();
        } catch (RestClientException ex) {
            throw new PaymentServiceUnavailableException("Payment service unavailable", ex);
        }
    }

    public static class PaymentServiceUnavailableException extends RuntimeException {
        public PaymentServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
