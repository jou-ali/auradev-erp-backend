package com.auradev.erp.sequence;

import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Gapless document-number generator using pessimistic row locking.
 *
 * <p>All three public methods share the same pattern:
 * <ol>
 *   <li>Compute the current Indian Financial Year (Apr–Mar) as a 4-char string.</li>
 *   <li>Acquire a pessimistic write lock on the sequence row for the given
 *       tenant + FY, inserting an empty row (lastNo = 0) if none exists yet.</li>
 *   <li>Increment {@code lastNo}, persist, and return the formatted string.</li>
 * </ol>
 *
 * <p>Because the sequence update happens inside the caller's transaction, a
 * rollback also reverts the counter — preserving gap-free numbering.</p>
 */
@Service
@RequiredArgsConstructor
public class SequenceServiceImpl implements SequenceService {

    private final BillSequenceRepository billSequenceRepository;
    private final PurchaseSequenceRepository purchaseSequenceRepository;
    private final CreditNoteSequenceRepository creditNoteSequenceRepository;
    private final TenantRepository tenantRepository;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    @Transactional
    public String nextBillNo(UUID tenantId) {
        String fy = currentFy();
        BillSequence seq = billSequenceRepository
                .findByTenantIdAndFyForUpdate(tenantId, fy)
                .orElseGet(() -> createBillSeq(tenantId, fy));
        seq.setLastNo(seq.getLastNo() + 1);
        billSequenceRepository.save(seq);
        return format(prefix(tenantId), fy, seq.getLastNo());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public String nextPurchaseNo(UUID tenantId) {
        String fy = currentFy();
        PurchaseSequence seq = purchaseSequenceRepository
                .findByTenantIdAndFyForUpdate(tenantId, fy)
                .orElseGet(() -> createPurchaseSeq(tenantId, fy));
        seq.setLastNo(seq.getLastNo() + 1);
        purchaseSequenceRepository.save(seq);
        return format(prefix(tenantId), fy, seq.getLastNo());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public String nextCreditNoteNo(UUID tenantId) {
        String fy = currentFy();
        CreditNoteSequence seq = creditNoteSequenceRepository
                .findByTenantIdAndFyForUpdate(tenantId, fy)
                .orElseGet(() -> createCreditNoteSeq(tenantId, fy));
        seq.setLastNo(seq.getLastNo() + 1);
        creditNoteSequenceRepository.save(seq);
        return format(prefix(tenantId), fy, seq.getLastNo());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Compute the Indian Financial Year key for today.
     *
     * <p>The Indian FY runs April–March.  If the current month is April (4) or
     * later, the FY start year is this calendar year; otherwise it is last year.
     * The returned string concatenates the last two digits of start and end years,
     * e.g. {@code "2526"} for FY 2025-26.</p>
     *
     * @return 4-char FY string, e.g. {@code "2526"}
     */
    private String currentFy() {
        LocalDate today = LocalDate.now();
        int startYear = (today.getMonthValue() >= 4) ? today.getYear() : today.getYear() - 1;
        int endYear = startYear + 1;
        return String.format("%02d%02d", startYear % 100, endYear % 100);
    }

    /**
     * Format the document number as {@code PREFIX-FY-NNNNN}.
     *
     * @param prefix  tenant bill-number prefix
     * @param fy      financial year string
     * @param counter the allocated counter value
     * @return formatted document number, e.g. {@code ERP-2526-00042}
     */
    private String format(String prefix, String fy, int counter) {
        return String.format("%s-%s-%05d", prefix, fy, counter);
    }

    /**
     * Resolve the tenant's bill-number prefix.
     *
     * @param tenantId the tenant UUID
     * @return the prefix string, defaulting to {@code "ERP"}
     */
    private String prefix(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
        String p = tenant.getBillNoPrefix();
        return (p != null && !p.isBlank()) ? p : "ERP";
    }

    // -------------------------------------------------------------------------
    // Factory methods for new sequence rows
    // -------------------------------------------------------------------------

    private BillSequence createBillSeq(UUID tenantId, String fy) {
        BillSequence s = new BillSequence();
        s.setTenantId(tenantId);
        s.setFy(fy);
        s.setLastNo(0);
        return billSequenceRepository.save(s);
    }

    private PurchaseSequence createPurchaseSeq(UUID tenantId, String fy) {
        PurchaseSequence s = new PurchaseSequence();
        s.setTenantId(tenantId);
        s.setFy(fy);
        s.setLastNo(0);
        return purchaseSequenceRepository.save(s);
    }

    private CreditNoteSequence createCreditNoteSeq(UUID tenantId, String fy) {
        CreditNoteSequence s = new CreditNoteSequence();
        s.setTenantId(tenantId);
        s.setFy(fy);
        s.setLastNo(0);
        return creditNoteSequenceRepository.save(s);
    }
}
