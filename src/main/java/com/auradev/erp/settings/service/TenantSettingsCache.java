package com.auradev.erp.settings.service;

import com.auradev.erp.settings.model.BillingConfig;
import com.auradev.erp.settings.model.TaxConfig;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived in-memory cache for tenant billing/tax settings.
 * Reduces repeated {@code tenant_settings} reads on hot paths like POS billing.
 */
@Component
public class TenantSettingsCache {

    private static final long TTL_MS = 60_000;

    public record Snapshot(BillingConfig billing, TaxConfig tax) {}

    private record Entry(Snapshot snapshot, long expiresAtMs) {}

    private final ConcurrentHashMap<UUID, Entry> cache = new ConcurrentHashMap<>();

    public Optional<Snapshot> get(UUID tenantId) {
        Entry entry = cache.get(tenantId);
        if (entry == null || entry.expiresAtMs <= System.currentTimeMillis()) {
            if (entry != null) {
                cache.remove(tenantId, entry);
            }
            return Optional.empty();
        }
        return Optional.of(entry.snapshot);
    }

    public void put(UUID tenantId, BillingConfig billing, TaxConfig tax) {
        cache.put(tenantId, new Entry(new Snapshot(billing, tax), System.currentTimeMillis() + TTL_MS));
    }

    public void evict(UUID tenantId) {
        cache.remove(tenantId);
    }
}
