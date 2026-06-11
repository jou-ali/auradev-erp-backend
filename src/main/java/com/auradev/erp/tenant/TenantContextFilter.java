package com.auradev.erp.tenant;

import com.auradev.erp.auth.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that populates {@link TenantContext} from the authenticated
 * JWT principal for the duration of each request.
 *
 * <p>Execution order:</p>
 * <ol>
 *   <li>Obtain the {@link Authentication} from the {@link SecurityContextHolder}.
 *       If no authentication exists (unauthenticated request that will be
 *       rejected downstream), the filter still clears the context in
 *       {@code finally}.</li>
 *   <li>Cast the principal to {@link UserPrincipal} and read
 *       {@link UserPrincipal#getTenantId()}.</li>
 *   <li>Skip setting the context for {@code SUPER_ADMIN} users — they operate
 *       across tenants and their {@code tenantId} may legitimately be
 *       {@code null}.</li>
 *   <li>Call {@link TenantContext#set(UUID)} then delegate to the rest of the
 *       filter chain.</li>
 *   <li>Clear the context in a {@code finally} block regardless of outcome.</li>
 * </ol>
 */
@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final SimpleGrantedAuthority SUPER_ADMIN_AUTHORITY =
            new SimpleGrantedAuthority("ROLE_SUPER_ADMIN");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UserPrincipal principal) {

                // SUPER_ADMIN operates across all tenants; skip tenant binding.
                boolean isSuperAdmin = authentication.getAuthorities()
                        .contains(SUPER_ADMIN_AUTHORITY);

                if (!isSuperAdmin) {
                    UUID tenantId = principal.getTenantId();
                    if (tenantId != null) {
                        TenantContext.set(tenantId);
                        log.trace("TenantContext set to {} for user {}",
                                tenantId, principal.getUsername());
                    } else {
                        log.warn("Non-SUPER_ADMIN principal {} has no tenantId; "
                                + "tenant context will be absent for this request",
                                principal.getUsername());
                    }
                } else {
                    log.trace("SUPER_ADMIN request — skipping tenant context binding");
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }
}
