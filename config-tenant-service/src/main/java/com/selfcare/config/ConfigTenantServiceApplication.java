package com.selfcare.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableCaching
@EnableMongoAuditing
@EnableMethodSecurity
public class ConfigTenantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigTenantServiceApplication.class, args);
    }
}
