package com.ridelink.payment.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
// Registers the authentication scheme displayed by the Swagger UI.
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class OpenAPIConfig {
}
