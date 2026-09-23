package com.ridelink.account.controller;

import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.exception.ApiException;
import com.ridelink.account.model.User;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests (Mockito-mocked repository, no real MongoDB needed) covering the
 * required successful and negative auth scenarios.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    @Test
    void registersNewPassengerSuccessfully() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Nimal Perera");
        req.setEmail("nimal@example.com");
        req.setPassword("password123");
        req.setRole("PASSENGER");

        when(userRepository.existsByEmail("nimal@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });

        var response = authController.register(req);

        assertEquals("nimal@example.com", response.getEmail());
        assertEquals("PASSENGER", response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsRegistrationWithDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Kamal");
        req.setEmail("kamal@example.com");
        req.setPassword("password123");
        req.setRole("DRIVER");

        when(userRepository.existsByEmail("kamal@example.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authController.register(req));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User existing = new User("Kamal", "kamal@example.com", encoder.encode("correct-password"), "DRIVER");
        existing.setId("user-2");

        when(userRepository.findByEmail("kamal@example.com")).thenReturn(Optional.of(existing));

        LoginRequest req = new LoginRequest();
        req.setEmail("kamal@example.com");
        req.setPassword("wrong-password");

        ApiException ex = assertThrows(ApiException.class, () -> authController.login(req));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void loginSucceedsAndReturnsToken() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User existing = new User("Nimal", "nimal@example.com", encoder.encode("password123"), "PASSENGER");
        existing.setId("user-3");

        when(userRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(existing));
        when(jwtUtil.generateToken("user-3", "PASSENGER", "nimal@example.com")).thenReturn("fake-jwt-token");

        LoginRequest req = new LoginRequest();
        req.setEmail("nimal@example.com");
        req.setPassword("password123");

        var response = authController.login(req);

        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("PASSENGER", response.getUser().getRole());
    }
}
