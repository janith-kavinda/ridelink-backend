package com.ridelink.ride.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Interservice communication #1: ride-service -> driver-service (synchronous REST).
 * Used when assigning a driver to a REQUESTED ride: finds the first available
 * (ONLINE) driver in the ride's service area.
 *
 * Synchronous REST was chosen here (over messaging) because ride assignment needs
 * an immediate answer to return to the passenger - see the report's comparison
 * of synchronous vs asynchronous communication for the justification.
 */
@Component
public class DriverServiceClient {

    private final RestTemplate restTemplate;
    private final String driverServiceUrl;

    public DriverServiceClient(RestTemplate restTemplate,
                                @Value("${services.driver-service.url}") String driverServiceUrl) {
        this.restTemplate = restTemplate;
        this.driverServiceUrl = driverServiceUrl;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> findAvailableDriver(String serviceArea, String bearerToken) {
        String url = UriComponentsBuilder.fromHttpUrl(driverServiceUrl + "/drivers")
                .queryParam("available", "true")
                .queryParam("serviceArea", serviceArea)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + bearerToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> drivers = response.getBody();
            if (drivers == null || drivers.isEmpty()) {
                return null; // no available driver
            }
            return drivers.get(0); // simple "first available" assignment rule
        } catch (RestClientException ex) {
            throw new DriverServiceUnavailableException("Driver service unavailable", ex);
        }
    }

    public static class DriverServiceUnavailableException extends RuntimeException {
        public DriverServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
