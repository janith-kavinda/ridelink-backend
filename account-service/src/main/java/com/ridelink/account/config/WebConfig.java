package com.ridelink.account.config;

import com.ridelink.account.security.JwtAuthFilter;
import com.ridelink.account.security.JwtUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilter(JwtUtil jwtUtil) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(new JwtAuthFilter(jwtUtil));
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }
}
