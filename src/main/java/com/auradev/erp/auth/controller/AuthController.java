package com.auradev.erp.auth.controller;

import com.auradev.erp.auth.dto.LoginRequest;
import com.auradev.erp.auth.dto.LoginResponse;
import com.auradev.erp.auth.dto.RefreshRequest;
import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.auth.service.AuthService;
import com.auradev.erp.user.dto.UserResponse;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST endpoints for authentication.
 *
 * <table>
 *   <caption>Endpoints</caption>
 *   <tr><th>Method</th><th>Path</th><th>Auth</th><th>Description</th></tr>
 *   <tr><td>POST</td><td>/login</td><td>public</td>
 *       <td>Exchange credentials for JWT tokens</td></tr>
 *   <tr><td>POST</td><td>/refresh</td><td>public</td>
 *       <td>Rotate refresh token and get new access token</td></tr>
 *   <tr><td>POST</td><td>/logout</td><td>authenticated</td>
 *       <td>Revoke the given refresh token</td></tr>
 *   <tr><td>GET</td><td>/me</td><td>authenticated</td>
 *       <td>Return the current user's profile</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Public endpoints
    // -------------------------------------------------------------------------

    /**
     * Authenticate with e-mail + password.
     *
     * @param req        the login credentials
     * @param httpRequest the underlying servlet request (used to extract IP)
     * @return HTTP 200 with {@link LoginResponse}
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {

        String ipAddress = resolveClientIp(httpRequest);
        LoginResponse response = authService.login(req, ipAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * Exchange a valid refresh token for a new token pair.
     *
     * @param req body containing the refresh token
     * @return HTTP 200 with new {@link LoginResponse}
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest req) {

        return ResponseEntity.ok(authService.refresh(req));
    }

    // -------------------------------------------------------------------------
    // Authenticated endpoints
    // -------------------------------------------------------------------------

    /**
     * Revoke the supplied refresh token (sign out from this device).
     *
     * @param req body containing the refresh token to revoke
     * @return HTTP 204 No Content
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Return the profile of the currently authenticated user.
     *
     * <p>A database round-trip is needed here because {@link UserPrincipal}
     * does not cache {@code name}, {@code lastLoginAt}, or {@code createdAt}.</p>
     *
     * @param principal the Spring Security principal injected by the JWT filter
     * @return HTTP 200 with the full {@link UserResponse}
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolve the client IP, honouring the {@code X-Forwarded-For} header when
     * running behind a proxy or load balancer.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For can be a comma-separated list; take the first entry.
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
