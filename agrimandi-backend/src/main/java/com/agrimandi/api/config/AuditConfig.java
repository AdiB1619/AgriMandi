package com.agrimandi.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import java.time.OffsetDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class AuditConfig {
    
    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
