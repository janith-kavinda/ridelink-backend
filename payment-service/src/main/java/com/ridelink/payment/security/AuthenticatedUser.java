package com.ridelink.payment.security;

// Holds the authenticated principal extracted from the JWT for this service.
public class AuthenticatedUser {
    private final String id; // user/account id from the token
    private final String role; // user role such as CUSTOMER or DRIVER
    private final String email; // email associated with the authenticated user

    public AuthenticatedUser(String id, String role, String email) {
        this.id = id;
        this.role = role;
        this.email = email;
    }

    public String getId() { return id; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
}
