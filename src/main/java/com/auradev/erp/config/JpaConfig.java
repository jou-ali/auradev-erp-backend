package com.auradev.erp.config;

import com.auradev.erp.auth.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA auditing configuration.
 *
 * <p>Reads the currently authenticated user's UUID from the
 * {@link SecurityContextHolder} and supplies it to Spring Data JPA's
 * {@code @CreatedBy} / {@code @LastModifiedBy} auditing mechanism.</p>
 *
 * <p>Returns {@link Optional#empty()} for unauthenticated requests (e.g.
 * public endpoints or internal scheduled tasks) so that the audit fields
 * are left null rather than causing an exception.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
                return Optional.empty();
            }

            return Optional.of(principal.getId());
        };
    }
}
