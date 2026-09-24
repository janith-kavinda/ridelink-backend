package com.ridelink.payment.security;

import java.util.Set;

import org.springframework.http.HttpStatus;

import com.ridelink.payment.exception.ApiException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Helper methods used by controllers to validate JWT-based access before processing a request.
 */
public final class AuthHelper {

    private AuthHelper() { }

    // Ensures a valid authenticated user exists on the request and returns the principal.
    public static AuthenticatedUser requireAuth(HttpServletRequest request) {
        Object attr = request.getAttribute(JwtAuthFilter.AUTH_USER_ATTR);
        if (attr == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return (AuthenticatedUser) attr;
    }

    // Retrieves the raw bearer token from the request after the JWT filter has processed it.
    public static String requireToken(HttpServletRequest request) {
        Object token = request.getAttribute(JwtAuthFilter.RAW_TOKEN_ATTR);
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        return (String) token;
    }

    // Verifies that the current user has at least one of the allowed roles.
    public static void requireRole(AuthenticatedUser user, String... roles) {
        if (!Set.of(roles).contains(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden: insufficient role");
        }
    }
}
