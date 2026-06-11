package com.auradev.erp.billing.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published via {@link org.springframework.context.ApplicationEventPublisher} when
 * a bill is successfully persisted.
 *
 * <p>The async event listener ({@link BillEventListener}) picks this up to trigger
 * receipt PDF generation and upload to Supabase Storage.  Because the listener runs
 * in a separate thread, the main billing transaction is already committed by the
 * time PDF generation begins.</p>
 *
 * @param billId    the UUID of the newly created bill
 * @param tenantId  the tenant that owns the bill
 * @param createdAt the timestamp at which the bill was committed
 */
public record BillCreatedEvent(
        UUID billId,
        UUID tenantId,
        Instant createdAt
) {}
