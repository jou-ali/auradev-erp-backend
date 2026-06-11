package com.auradev.erp.creditnote.service;

import com.auradev.erp.billing.entity.Bill;
import com.auradev.erp.billing.repository.BillRepository;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.error.TenantAccessException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.creditnote.dto.CreditNoteItemRequest;
import com.auradev.erp.creditnote.dto.CreditNoteItemResponse;
import com.auradev.erp.creditnote.dto.CreditNoteResponse;
import com.auradev.erp.creditnote.dto.CreateCreditNoteRequest;
import com.auradev.erp.creditnote.entity.CreditNote;
import com.auradev.erp.creditnote.entity.CreditNoteItem;
import com.auradev.erp.creditnote.repository.CreditNoteRepository;
import com.auradev.erp.inventory.entity.MovementReason;
import com.auradev.erp.inventory.entity.MovementRefType;
import com.auradev.erp.inventory.service.InventoryService;
import com.auradev.erp.party.entity.Customer;
import com.auradev.erp.party.repository.CustomerRepository;
import com.auradev.erp.sequence.SequenceService;
import com.auradev.erp.settings.repository.TenantSettingsRepository;
import com.auradev.erp.tax.service.DiscountMode;
import com.auradev.erp.tax.service.GstCalculatorService;
import com.auradev.erp.tax.service.GstCalculatorService.BillTaxOutput;
import com.auradev.erp.tax.service.GstCalculatorService.LineInput;
import com.auradev.erp.tax.service.GstCalculatorService.LineOutput;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for credit note (sales return) management.
 *
 * <p>Creating a credit note is transactional — stock is restored, the customer's
 * credit balance is updated, and the credit note persisted atomically.  A
 * rollback leaves all three resources unchanged.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CreditNoteService {

    private final CreditNoteRepository   creditNoteRepository;
    private final BillRepository         billRepository;
    private final ProductRepository      productRepository;
    private final CustomerRepository     customerRepository;
    private final InventoryService       inventoryService;
    private final GstCalculatorService   gstCalculatorService;
    private final SequenceService        sequenceService;
    private final TenantRepository       tenantRepository;
    private final TenantSettingsRepository tenantSettingsRepository;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<CreditNoteResponse> list(Pageable pageable) {
        UUID tenantId = TenantContext.require();
        Page<CreditNote> page = creditNoteRepository.findByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public CreditNoteResponse get(UUID id) {
        UUID tenantId = TenantContext.require();
        CreditNote cn = creditNoteRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("CreditNote", id));
        return toResponse(cn);
    }

    @Transactional(readOnly = true)
    public List<CreditNoteResponse> listByBill(UUID billId) {
        UUID tenantId = TenantContext.require();
        Bill bill = billRepository.findById(billId)
                .filter(b -> tenantId.equals(b.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Bill", billId));
        return creditNoteRepository.findByOriginalBillId(bill.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Create credit note
    // -------------------------------------------------------------------------

    /**
     * Issue a credit note against an original bill.
     *
     * <ol>
     *   <li>Validate original bill exists and belongs to this tenant.</li>
     *   <li>Load returned products; verify they are active and tenant-scoped.</li>
     *   <li>Compute GST using the same intra/inter-state logic as billing.</li>
     *   <li>Persist credit note + items.</li>
     *   <li>Restore stock (positive RETURN movement per line).</li>
     *   <li>Credit customer balance if a customer is associated.</li>
     * </ol>
     */
    public CreditNoteResponse create(CreateCreditNoteRequest req, UUID issuedBy) {
        UUID tenantId = TenantContext.require();

        // ------------------------------------------------------------------
        // Step 1: Validate original bill
        // ------------------------------------------------------------------
        Bill originalBill = billRepository.findById(req.originalBillId())
                .filter(b -> tenantId.equals(b.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Bill", req.originalBillId()));

        // ------------------------------------------------------------------
        // Step 2: Load products (no write lock needed — we're adding stock)
        // ------------------------------------------------------------------
        List<UUID> productIds = req.items().stream()
                .map(CreditNoteItemRequest::productId)
                .distinct()
                .toList();

        Map<UUID, Product> productMap = productIds.stream()
                .map(id -> productRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Product", id)))
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (Product p : productMap.values()) {
            if (!tenantId.equals(p.getTenantId())) {
                throw new TenantAccessException("Product " + p.getId() + " does not belong to this tenant");
            }
        }

        // ------------------------------------------------------------------
        // Step 3: GST calculation (mirrors the original bill's logic)
        // ------------------------------------------------------------------
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));

        boolean priceIncludesTax = tenantSettingsRepository.findById(tenantId)
                .map(s -> s.isPriceIncludesTax())
                .orElse(false);

        Customer customer = originalBill.getCustomer();
        boolean intraState = customer == null
                || customer.getStateCode() == null
                || customer.getStateCode().isBlank()
                || customer.getStateCode().equals(tenant.getStateCode());

        List<LineInput> lineInputs = req.items().stream()
                .map(item -> {
                    Product product = productMap.get(item.productId());
                    return new LineInput(
                            item.unitPrice(),
                            item.quantity(),
                            BigDecimal.ZERO,
                            product.getGstRate());
                })
                .toList();

        BillTaxOutput taxOutput = gstCalculatorService.calculate(
                lineInputs, BigDecimal.ZERO, DiscountMode.AMOUNT, priceIncludesTax, intraState);

        // ------------------------------------------------------------------
        // Step 4: Reserve credit note number and persist
        // ------------------------------------------------------------------
        String creditNoteNo = sequenceService.nextCreditNoteNo(tenantId);

        CreditNote creditNote = new CreditNote();
        creditNote.setTenantId(tenantId);
        creditNote.setCreditNoteNo(creditNoteNo);
        creditNote.setOriginalBill(originalBill);
        creditNote.setCustomer(customer);
        creditNote.setReason(req.reason());
        creditNote.setSubtotal(taxOutput.subtotal());
        creditNote.setCgstTotal(taxOutput.cgstTotal());
        creditNote.setSgstTotal(taxOutput.sgstTotal());
        creditNote.setIgstTotal(taxOutput.igstTotal());
        creditNote.setGrandTotal(taxOutput.grandTotal());
        creditNote.setCreatedBy(issuedBy);

        creditNote = creditNoteRepository.save(creditNote);

        // ------------------------------------------------------------------
        // Step 5: Persist credit note items
        // ------------------------------------------------------------------
        List<LineOutput> lineOutputs = taxOutput.lines();
        List<CreditNoteItem> items = new ArrayList<>(req.items().size());

        for (int i = 0; i < req.items().size(); i++) {
            CreditNoteItemRequest itemReq = req.items().get(i);
            Product product = productMap.get(itemReq.productId());
            LineOutput lineOut = lineOutputs.get(i);

            CreditNoteItem item = new CreditNoteItem();
            item.setCreditNote(creditNote);
            item.setProduct(product);
            item.setProductNameSnapshot(product.getName());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            item.setTaxableValue(lineOut.taxableValue());
            item.setGstRate(product.getGstRate());
            item.setCgstAmount(lineOut.cgst());
            item.setSgstAmount(lineOut.sgst());
            item.setIgstAmount(lineOut.igst());
            item.setLineTotal(lineOut.lineTotal());
            item.setTenantId(tenantId);
            items.add(item);
        }

        creditNote.getItems().addAll(items);
        creditNote = creditNoteRepository.save(creditNote);

        // ------------------------------------------------------------------
        // Step 6: Restore stock (positive delta per line)
        // ------------------------------------------------------------------
        final UUID creditNoteId = creditNote.getId();
        for (CreditNoteItemRequest itemReq : req.items()) {
            inventoryService.recordMovement(
                    itemReq.productId(),
                    itemReq.quantity(),
                    MovementReason.RETURN,
                    MovementRefType.CREDIT_NOTE,
                    creditNoteId,
                    "Credit note " + creditNoteNo);
        }

        // ------------------------------------------------------------------
        // Step 7: Credit customer balance (grand total credited back)
        // ------------------------------------------------------------------
        if (customer != null) {
            customer.setCreditBalance(
                    customer.getCreditBalance().add(taxOutput.grandTotal()));
            customerRepository.save(customer);
        }

        log.info("Credit note created: id={} no={} originalBill={} tenant={}",
                creditNote.getId(), creditNoteNo, req.originalBillId(), tenantId);

        return toResponse(creditNote);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private CreditNoteResponse toResponse(CreditNote cn) {
        Bill bill = cn.getOriginalBill();
        Customer cust = cn.getCustomer();

        List<CreditNoteItemResponse> items = cn.getItems().stream()
                .map(i -> new CreditNoteItemResponse(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProductNameSnapshot(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTaxableValue(),
                        i.getGstRate(),
                        i.getCgstAmount(),
                        i.getSgstAmount(),
                        i.getIgstAmount(),
                        i.getLineTotal()))
                .collect(Collectors.toList());

        return new CreditNoteResponse(
                cn.getId(),
                cn.getCreditNoteNo(),
                bill != null ? bill.getId() : null,
                bill != null ? bill.getBillNo() : null,
                cust != null ? cust.getId() : null,
                cust != null ? cust.getName() : null,
                cn.getReason(),
                cn.getSubtotal(),
                cn.getCgstTotal(),
                cn.getSgstTotal(),
                cn.getIgstTotal(),
                cn.getGrandTotal(),
                items,
                cn.getCreatedAt(),
                cn.getCreatedBy()
        );
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.auradev.erp.auth.security.UserPrincipal up) {
            return up.getId();
        }
        return null;
    }
}
