package com.auradev.erp.config;

import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.entity.UserStatus;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Tenant tenant;
        if (tenantRepository.count() > 0) {
            tenant = tenantRepository.findAll().get(0);
            log.debug("Seed — tenant already present (id={}), checking admin user", tenant.getId());
        } else {
            log.info("No tenants found — seeding initial tenant and admin user");
            tenant = new Tenant();
            tenant.setName("Nenjankod Supermarket");
            tenant.setPhone("0820 256 7711");
            tenant.setStateCode("29");
            tenant.setBillNoPrefix("NJK");
            tenant.setActive(true);
            tenant = tenantRepository.save(tenant);
        }

        if (userRepository.findByEmail("admin@nenjankod.in").isPresent()) {
            log.debug("Seed skipped — admin user already present");
            return;
        }

        User admin = new User();
        admin.setTenantId(tenant.getId());
        admin.setName("Admin");
        admin.setEmail("admin@nenjankod.in");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRole(UserRole.TENANT_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        log.info("Seeded tenant '{}' (id={}) | admin: admin@nenjankod.in / Admin@123",
                tenant.getName(), tenant.getId());
    }
}
