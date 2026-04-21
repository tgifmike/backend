package com.backend.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


import java.util.Optional;
import java.util.UUID;

import com.backend.backend.config.UserContext;

@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> Optional.ofNullable(UserContext.getCurrentUser());
    }
}


