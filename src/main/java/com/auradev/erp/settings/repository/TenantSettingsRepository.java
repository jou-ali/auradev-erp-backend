package com.auradev.erp.settings.repository;

import com.auradev.erp.settings.entity.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Data-access layer for {@link TenantSettings}.
 */
@Repository
public interface TenantSettingsRepository extends JpaRepository<TenantSettings, UUID> {
}
