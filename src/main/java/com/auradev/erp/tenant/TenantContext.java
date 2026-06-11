package com.auradev.erp.tenant;

import java.util.UUID;

/**
 * Thread-local holder for the current request's tenant identifier.
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>Set by {@link TenantContextFilter} early in the filter chain.</li>
 *   <li>Read by repositories, services, and Hibernate filters throughout
 *       the request.</li>
 *   <li>Cleared in a {@code finally} block by {@link TenantContextFilter}
 *       to prevent leakage across pooled threads.</li>
 * </ol>
 *
 * <p>This class must never be instantiated; use its static methods directly.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();

    // Prevent instantiation
    private TenantContext() {
    }

    /**
     * Store {@code id} as the tenant for the current thread.
     *
     * @param id the tenant UUID (may be {@code null} for SUPER_ADMIN requests)
     */
    public static void set(UUID id) {
        TENANT.set(id);
    }

    /**
     * Return the tenant UUID bound to the current thread, or {@code null} if
     * none has been set (e.g. for SUPER_ADMIN requests or during startup).
     *
     * @return the current tenant UUID, or {@code null}
     */
    public static UUID get() {
        return TENANT.get();
    }

    /**
     * Remove the tenant binding from the current thread.
     * Always call this in a {@code finally} block after the request completes.
     */
    public static void clear() {
        TENANT.remove();
    }

    /**
     * Return the tenant UUID bound to the current thread, throwing if absent.
     *
     * <p>Use this in application code that must not proceed without a tenant
     * context (e.g. data-access layer operations on tenant-scoped tables).</p>
     *
     * @return the current tenant UUID (never {@code null})
     * @throws IllegalStateException if no tenant has been set for this thread
     */
    public static UUID require() {
        UUID id = TENANT.get();
        if (id == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return id;
    }
}
