package com.donquijote.biblioteca;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Permite solamente los orígenes del frontend configurados por el administrador. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:${APP_CORS_ALLOWED_ORIGINS:http://localhost:5500,http://127.0.0.1:5500,http://localhost:8000,http://127.0.0.1:8000}}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept")
                .maxAge(3600);
    }
}
