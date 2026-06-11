package com.auradev.erp.auth.security;

import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.entity.UserStatus;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security {@link UserDetailsService} backed by the {@link UserRepository}.
 *
 * <p>Uses {@link UserRepository#findByEmail(String)} so that SUPER_ADMIN accounts
 * (which have a {@code null} tenant_id) are resolved correctly.  The tenant filter
 * must therefore be disabled for this query, which is achieved by not enabling it
 * in this read-only service method.</p>
 *
 * <p>Inactive users are rejected here rather than relying solely on
 * {@link UserPrincipal#isAccountNonLocked()} so that the error message is
 * explicit in the logs.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their email address.
     *
     * @param email the email supplied as the username
     * @return a populated {@link UserPrincipal}
     * @throws UsernameNotFoundException if no user with this email exists or the account is inactive
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("No user found with email: " + email));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UsernameNotFoundException("User account is inactive: " + email);
        }

        return UserPrincipal.from(user);
    }
}
