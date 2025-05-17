package ru.practicum.explorewithme.main.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@Profile("!test")
@SuppressWarnings("unused")
public class JpaAuditingConfig {
}