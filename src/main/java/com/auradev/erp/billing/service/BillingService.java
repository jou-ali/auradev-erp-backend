package com.auradev.erp.billing.service;

import com.auradev.erp.audit.service.AuditService;
import com.auradev.erp.billing.dto.BillItemRequest;
import com.auradev.erp.billing.dto.BillItemResponse;
import com.auradev.erp.billing.dto.BillResponse;
import com.auradev.erp.billing.dto.CreateBillRequest;
import com.auradev.erp.billing.dto.PaymentRequest;
import com.auradev.erp.billing.dto.PaymentResponse;
import com.auradev.erp.billing.entity.Bill;
import com.auradev.erp.billing.entity.BillItem;
import com.auradev.erp.billing.entity.BillStatus;
import com.auradev.erp.billing.entity.DiscountMode;
import com.auradev.erp.billing.entity.Payment;
import com.auradev.erp.billing.entity.PaymentMethod;
import com.auradev.erp.billing.entity.PaymentStatus;
import com.auradev.erp.billing.event.BillCreatedEvent;
import com.auradev.erp.billing.repository.BillItemRepository;
import com.auradev.erp.billing.repository.BillRepository;
import com.auradev.erp.billing.repository.PaymentRepository;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.error.TenantAccessException;
import com.auradev.erp.inventory.entity.MovementReason;
import com.auradev.erp.inventory.entity.MovementRefType;
import com.auradev.erp.inventory.service.InventoryService;
import com.auradev.erp.party.entity.Customer;
import com.auradev.erp.party.entity.CustomerType;
import com.auradev.erp.party.repository.CustomerRepository;
import com.auradev.erp.sequence.SequenceService;
import com.auradev.erp.settings.entity.TenantSettings;
import com.auradev.erp.settings.repository.TenantSettingsRepository;
import com.auradev.erp.tax.service.GstCalculatorService;
import com.auradev.erp.tax.service.GstCalculatorService.BillTaxOutput;
import com.auradev.erp.tax.service.GstCalculatorService.LineInput;
import com.auradev.erp.tax.service.GstCalculatorService.LineOutput;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Core billing service responsible for the complete POST /bills transactional flow.
 *
 * <h2>Transaction semantics</h2>
 * <p>The entire {@link #createBill} method runs in a single ACID transaction.
 * Stock movements are written within the same transaction so that any oversell
 * rejection rolls back the bill, its items, and all payment rows atomically.</p>
 *
 * <h2>Idempotency</h2>
 * <p>If an {@code Idempotency-Key} header is present, the service looks up an
 * existing bill with the same key and returns it without re-processing.  This
 * allows safe retries from unreliable networks or client crashes.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BillingService {

    private final BillRepository              billRepository;
    private final BillItemRepository          billItemRepository;
    private final PaymentRepository           paymentRepository;
    private final ProductRepository           productRepository;
    private final CustomerRepository          customerRepository;
    private final UserRepository              userRepository;
    private final InventoryService            inventoryService;
    private final GstCalculatorService        gstCalculatorService;
    private final SequenceService             sequenceService;
    private final AuditService               auditService;
    private final TenantRepository            tenantRepository;
    private final TenantSettingsRepository    tenantSettingsRepository;
    private final ApplicationEventPublisher   eventPublisher;

    // =========================================================================
    // Create bill — the critical transactional flow
    // =========================================================================

    /**
     * Create a new sales bill.
     *
     * <p>The full transactional flow is:</p>
     * <ol>
     *   <li>Idempotency check — return existing bill if key already used.</li>
     *   <li>Validate items not empty; customer exists and belongs to this tenant.</li>
     *   <li>Validate credit: CREDIT payment requires a non-walk-in customer.</li>
     *   <li>Load all products with pessimistic write lock; verify all active + tenant-scoped.</li>
     *   <li>Determine intra/inter-state GST from customer.stateCode vs tenant.stateCode.</li>
     *   <li>Call {@link GstCalculatorService} to compute tax breakdown per line and bill totals.</li>
     *   <li>Reserve bill number via {@link SequenceService}.</li>
     *   <li>Persist {@link Bill}.</li>
     *   <li>Persist {@link BillItem}s with product snapshot (name, SKU, HSN).</li>
     *   <li>Persist {@link Payment} rows; split payments stored as SPLIT_COMPONENT rows.</li>
     *   <li>For CREDIT payments: increment customer.creditBalance; save customer.</li>
     *   <li>Record stock movements; throw {@code INSUFFICIENT_STOCK} on oversell — entire tx rolls back.</li>
     *   <li>Write audit log entry (async, non-throwing).</li>
     *   <li>Publish {@link BillCreatedEvent} for async PDF generation.</li>
     *   <li>Return fully-populated {@link BillResponse}.</li>
     * </ol>
     *
     * @param req            the bill creation request
     * @param cashierId      UUID of the authenticated cashier
     * @param idempotencyKey optional deduplication key from the HTTP header
     * @return the created (or replayed) bill response
     */
    public BillResponse createBill(CreateBillRequest req, UUID cashierId, String idempotencyKey) {
        UUID tenantId = TenantContext.require();

        // Prefer the idempotencyKey embedded in the request body; fall back to the header value.
        String effectiveIdempotencyKey = (req.idempotencyKey() != null && !req.idempotencyKey().isBlank())
                ? req.idempotencyKey()
                : idempotencyKey;

        // -------------------------------------------------------------------
        // Step 1: Idempotency check
        // -------------------------------------------------------------------
        if (effectiveIdempotencyKey != null && !effectiveIdempotencyKey.isBlank()) {
            var existing = billRepository.findByIdempotencyKeyAndTenantId(effectiveIdempotencyKey, tenantId);
            if (existing.isPresent()) {
                log.debug("Idempotent replay for key={} billId={}", effectiveIdempotencyKey, existing.get().getId());
                return mapToResponse(existing.get());
            }
        }

        // -------------------------------------------------------------------
        // Step 2: Validate items and customer
        // -------------------------------------------------------------------
        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessException("EMPTY_BILL", "A bill must contain at least one item");
        }

        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", req.customerId()));

        if (!tenantId.equals(customer.getTenantId())) {
            throw new TenantAccessException("Customer does not belong to this tenant");
        }

        // -------------------------------------------------------------------
        // Step 3: Validate credit payments require a non-walkin customer
        // -------------------------------------------------------------------
        boolean hasCredit = req.payments().stream()
                .anyMatch(p -> p.method() == PaymentMethod.CREDIT);
        if (hasCredit && customer.getType() == CustomerType.WALKIN) {
            throw new BusinessException("CREDIT_NOT_ALLOWED",
                    "Credit payments are not available for walk-in customers");
        }

        // -------------------------------------------------------------------
        // Step 4: Load all products FOR UPDATE (pessimistic write lock)
        // -------------------------------------------------------------------
        List<UUID> productIds = req.items().stream()
                .map(BillItemRequest::productId)
                .distinct()
                .toList();

        Map<UUID, Product> productMap = productIds.stream()
                .map(id -> productRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new EntityNotFoundException("Product", id)))
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Verify all products are active and belong to this tenant
        for (Product p : productMap.values()) {
            if (!tenantId.equals(p.getTenantId())) {
                throw new TenantAccessException("Product " + p.getId() + " does not belong to this tenant");
            }
            if (!p.isActive()) {
                throw new BusinessException("PRODUCT_INACTIVE",
                        "Product '" + p.getName() + "' is not available for sale");
            }
        }

        // -------------------------------------------------------------------
        // Step 5: Load tenant settings — intraState and priceIncludesTax
        // -------------------------------------------------------------------
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));

        boolean priceIncludesTax = tenantSettingsRepository.findById(tenantId)
                .map(TenantSettings::isPriceIncludesTax)
                .orElse(false);

        // intraState = true when customer has no state code (default intra),
        // or when their state code matches the tenant's state code.
        boolean intraState = customer.getStateCode() == null
                || customer.getStateCode().isBlank()
                || customer.getStateCode().equals(tenant.getStateCode());

        // -------------------------------------------------------------------
        // Step 6: Build LineInput list and call GstCalculatorService
        // -------------------------------------------------------------------
        List<LineInput> lineInputs = req.items().stream()
                .map(item -> {
                    Product product = productMap.get(item.productId());
                    BigDecimal unitPrice = (item.unitPrice() != null)
                            ? item.unitPrice()
                            : product.getSellingPrice();
                    BigDecimal lineDiscount = (item.lineDiscount() != null)
                            ? item.lineDiscount()
                            : BigDecimal.ZERO;
                    return new LineInput(unitPrice, item.quantity(), lineDiscount, product.getGstRate());
                })
                .toList();

        BigDecimal billDiscountValue = (req.billDiscount() != null)
                ? req.billDiscount()
                : BigDecimal.ZERO;

        // Map the billing entity DiscountMode to the tax service DiscountMode
        com.auradev.erp.tax.service.DiscountMode taxDiscountMode =
                (req.discountMode() == DiscountMode.PERCENT)
                        ? com.auradev.erp.tax.service.DiscountMode.PERCENT
                        : com.auradev.erp.tax.service.DiscountMode.AMOUNT;

        BillTaxOutput taxOutput = gstCalculatorService.calculate(
                lineInputs, billDiscountValue, taxDiscountMode, priceIncludesTax, intraState);

        // -------------------------------------------------------------------
        // Step 7: Reserve bill number
        // -------------------------------------------------------------------
        String billNo = sequenceService.nextBillNo(tenantId);

        // -------------------------------------------------------------------
        // Step 8: Build and save Bill entity
        // -------------------------------------------------------------------
        User cashierUser = userRepository.getReferenceById(cashierId);

        Bill bill = new Bill();
        bill.setTenantId(tenantId);
        bill.setBillNo(billNo);
        bill.setCustomer(customer);
        bill.setCashier(cashierUser);
        bill.setPlaceOfSupplyState(
                (customer.getStateCode() != null && !customer.getStateCode().isBlank())
                        ? customer.getStateCode()
                        : tenant.getStateCode());
        bill.setSubtotal(taxOutput.subtotal());
        bill.setBillDiscount(taxOutput.billDiscountApplied());
        bill.setDiscountMode(req.discountMode() != null ? req.discountMode() : DiscountMode.AMOUNT);
        bill.setCgstTotal(taxOutput.cgstTotal());
        bill.setSgstTotal(taxOutput.sgstTotal());
        bill.setIgstTotal(taxOutput.igstTotal());
        bill.setRoundOff(taxOutput.roundOff());
        bill.setGrandTotal(taxOutput.grandTotal());
        bill.setStatus(BillStatus.COMPLETED);
        bill.setPaymentStatus(determineBillPaymentStatus(req));
        bill.setIdempotencyKey(effectiveIdempotencyKey);
        bill.setCreatedBy(cashierId);

        bill = billRepository.save(bill);

        // -------------------------------------------------------------------
        // Step 9: Build and save BillItem entities (snapshot product state)
        // -------------------------------------------------------------------
        List<LineOutput> lineOutputs = taxOutput.lines();
        List<BillItem> billItems = new ArrayList<>(req.items().size());

        for (int i = 0; i < req.items().size(); i++) {
            BillItemRequest itemReq = req.items().get(i);
            Product product = productMap.get(itemReq.productId());
            LineInput lineIn = lineInputs.get(i);
            LineOutput lineOut = lineOutputs.get(i);

            BillItem item = new BillItem();
            item.setBill(bill);
            item.setProduct(product);
            item.setProductNameSnapshot(product.getName());
            item.setSkuSnapshot(product.getSku());
            item.setHsnSnapshot(product.getHsnCode());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(lineIn.unitPrice());
            item.setLineDiscount(lineIn.lineDiscount());
            item.setTaxableValue(lineOut.taxableValue());
            item.setGstRate(product.getGstRate());
            item.setCgstAmount(lineOut.cgst());
            item.setSgstAmount(lineOut.sgst());
            item.setIgstAmount(lineOut.igst());
            item.setLineTotal(lineOut.lineTotal());

            billItems.add(item);
        }

        billItemRepository.saveAll(billItems);

        // -------------------------------------------------------------------
        // Step 10: Save Payment rows
        //          Split = multiple rows each stored as SPLIT_COMPONENT
        // -------------------------------------------------------------------
        boolean isSplit = req.payments().size() > 1;
        List<Payment> payments = new ArrayList<>(req.payments().size());

        for (PaymentRequest payReq : req.payments()) {
            Payment payment = new Payment();
            payment.setBill(bill);
            payment.setMethod(isSplit ? PaymentMethod.SPLIT_COMPONENT : payReq.method());
            payment.setAmount(payReq.amount());
            payment.setTendered(payReq.tendered());
            if (payReq.method() == PaymentMethod.CASH && payReq.tendered() != null) {
                BigDecimal change = payReq.tendered().subtract(payReq.amount());
                payment.setChangeDue(change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO);
            }
            payment.setReference(payReq.reference());
            payments.add(payment);
        }

        paymentRepository.saveAll(payments);

        // -------------------------------------------------------------------
        // Step 11: For CREDIT payments, increment customer.creditBalance
        // -------------------------------------------------------------------
        BigDecimal creditAmount = req.payments().stream()
                .filter(p -> p.method() == PaymentMethod.CREDIT)
                .map(PaymentRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            customer.setCreditBalance(customer.getCreditBalance().add(creditAmount));
            customerRepository.save(customer);
        }

        // -------------------------------------------------------------------
        // Step 12: Record stock movements (SALE, negative delta per line)
        //          Oversell throws BusinessException → entire tx rolls back
        // -------------------------------------------------------------------
        final UUID finalBillId = bill.getId();
        for (int i = 0; i < req.items().size(); i++) {
            BillItemRequest itemReq = req.items().get(i);
            Product product = productMap.get(itemReq.productId());
            BigDecimal delta = itemReq.quantity().negate();

            try {
                inventoryService.recordMovement(
                        product.getId(),
                        delta,
                        MovementReason.SALE,
                        MovementRefType.BILL,
                        finalBillId,
                        null
                );
            } catch (BusinessException ex) {
                if ("INSUFFICIENT_STOCK".equals(ex.getCode())) {
                    // Re-throw with the product-name + unit details required by the spec
                    throw new BusinessException("INSUFFICIENT_STOCK",
                            "Product '" + product.getName() + "' has only "
                                    + product.getCurrentStock() + " "
                                    + product.getUnit().name().toLowerCase() + " available");
                }
                throw ex;
            }
        }

        // -------------------------------------------------------------------
        // Step 13: Write audit log (non-throwing — must not roll back tx)
        // -------------------------------------------------------------------
        try {
            auditService.log(
                    tenantId,
                    resolveCurrentUserId(),
                    "BILL_CREATED",
                    "Bill",
                    bill.getId(),
                    null
            );
        } catch (Exception ex) {
            log.warn("Audit log write failed for bill {} — non-fatal", bill.getId(), ex);
        }

        // -------------------------------------------------------------------
        // Step 14: Publish BillCreatedEvent (async PDF generation)
        // -------------------------------------------------------------------
        eventPublisher.publishEvent(new BillCreatedEvent(bill.getId(), tenantId, Instant.now()));

        // -------------------------------------------------------------------
        // Step 15: Return BillResponse with full tax breakup
        // -------------------------------------------------------------------
        bill.getItems().addAll(billItems);
        bill.getPayments().addAll(payments);
        return mapToResponse(bill);
    }

    // =========================================================================
    // Read operations
    // =========================================================================

    /**
     * Return a single bill by its UUID, scoped to the current tenant.
     *
     * @param id the bill UUID
     * @return the bill response
     * @throws EntityNotFoundException if no bill with this ID exists in the tenant
     */
    @Transactional(readOnly = true)
    public BillResponse getBill(UUID id) {
        UUID tenantId = TenantContext.require();
        Bill bill = billRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Bill", id));
        return mapToResponse(bill);
    }

    /**
     * Return a paginated list of bills for the current tenant, optionally filtered
     * by status.
     *
     * @param statusFilter if non-null, only bills with this status are returned
     * @param pageable     pagination and sort parameters
     * @return page of bill responses
     */
    @Transactional(readOnly = true)
    public Page<BillResponse> listBills(BillStatus statusFilter, Pageable pageable) {
        UUID tenantId = TenantContext.require();
        Page<Bill> page = (statusFilter != null)
                ? billRepository.findByTenantIdAndStatus(tenantId, statusFilter, pageable)
                : billRepository.findByTenantId(tenantId, pageable);
        return page.map(this::mapToResponse);
    }

    // =========================================================================
    // Void bill
    // =========================================================================

    /**
     * Void a bill: set status to VOID, reverse stock movements, write audit.
     *
     * <p>Stock is reversed by posting a RETURN movement (positive delta) for
     * each line item.  Payment status is also set to VOID.</p>
     *
     * @param id the bill UUID to void
     * @return the updated bill response
     * @throws BusinessException       if the bill is already VOID
     * @throws EntityNotFoundException if the bill does not exist
     */
    public BillResponse voidBill(UUID id) {
        UUID tenantId = TenantContext.require();

        Bill bill = billRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Bill", id));

        if (bill.getStatus() == BillStatus.VOID) {
            throw new BusinessException("ALREADY_VOIDED",
                    "Bill " + bill.getBillNo() + " is already voided");
        }

        bill.setStatus(BillStatus.VOID);
        bill.setPaymentStatus(PaymentStatus.VOID);
        billRepository.save(bill);

        // Reverse stock for each line item
        List<BillItem> items = billItemRepository.findByBillId(id);
        for (BillItem item : items) {
            UUID productId = (item.getProduct() != null)
                    ? item.getProduct().getId()
                    : null;
            if (productId != null) {
                inventoryService.recordMovement(
                        productId,
                        item.getQuantity(),          // positive delta = stock restored
                        MovementReason.RETURN,
                        MovementRefType.BILL,
                        id,
                        "Void of bill " + bill.getBillNo()
                );
            }
        }

        try {
            auditService.log(
                    tenantId,
                    resolveCurrentUserId(),
                    "BILL_VOIDED",
                    "Bill",
                    id,
                    null
            );
        } catch (Exception ex) {
            log.warn("Audit log write failed for void of bill {} — non-fatal", id, ex);
        }

        return mapToResponse(bill);
    }

    // =========================================================================
    // Hold bill
    // =========================================================================

    /**
     * Put a bill on hold.  Stock is NOT reversed on hold; it remains committed.
     *
     * @param id the bill UUID to hold
     * @return the updated bill response
     * @throws BusinessException if the bill is VOID
     */
    public BillResponse holdBill(UUID id) {
        UUID tenantId = TenantContext.require();

        Bill bill = billRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Bill", id));

        if (bill.getStatus() == BillStatus.VOID) {
            throw new BusinessException("BILL_VOIDED", "Cannot hold a voided bill");
        }

        bill.setStatus(BillStatus.HELD);
        billRepository.save(bill);

        return mapToResponse(bill);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Determine the {@link PaymentStatus} from the payment lines in the request.
     */
    private PaymentStatus determineBillPaymentStatus(CreateBillRequest req) {
        boolean hasCredit = req.payments().stream()
                .anyMatch(p -> p.method() == PaymentMethod.CREDIT);
        boolean hasNonCredit = req.payments().stream()
                .anyMatch(p -> p.method() != PaymentMethod.CREDIT);

        if (hasCredit && hasNonCredit) {
            return PaymentStatus.PARTIAL;
        }
        if (hasCredit) {
            return PaymentStatus.CREDIT;
        }
        return PaymentStatus.PAID;
    }

    /** Map a {@link Bill} entity (with lazily-loaded items/payments) to a {@link BillResponse}. */
    private BillResponse mapToResponse(Bill bill) {
        UUID   customerId   = (bill.getCustomer() != null) ? bill.getCustomer().getId()   : null;
        String customerName = (bill.getCustomer() != null) ? bill.getCustomer().getName() : null;
        UUID   cashierId    = (bill.getCashier()  != null) ? bill.getCashier().getId()    : null;
        String cashierName  = (bill.getCashier()  != null) ? bill.getCashier().getName()  : null;

        List<BillItemResponse> itemResponses = bill.getItems().stream()
                .map(item -> {
                    UUID productId = (item.getProduct() != null) ? item.getProduct().getId() : null;
                    return new BillItemResponse(
                            item.getId(),
                            productId,
                            item.getProductNameSnapshot(),
                            item.getSkuSnapshot(),
                            item.getHsnSnapshot(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getLineDiscount(),
                            item.getTaxableValue(),
                            item.getGstRate(),
                            item.getCgstAmount(),
                            item.getSgstAmount(),
                            item.getIgstAmount(),
                            item.getLineTotal()
                    );
                })
                .toList();

        List<PaymentResponse> paymentResponses = bill.getPayments().stream()
                .map(p -> new PaymentResponse(
                        p.getId(),
                        p.getMethod(),
                        p.getAmount(),
                        p.getTendered(),
                        p.getChangeDue(),
                        p.getReference()
                ))
                .toList();

        return new BillResponse(
                bill.getId(),
                bill.getBillNo(),
                bill.getTenantId(),
                customerId,
                customerName,
                cashierId,
                cashierName,
                bill.getPlaceOfSupplyState(),
                bill.getSubtotal(),
                bill.getBillDiscount(),
                bill.getDiscountMode(),
                bill.getCgstTotal(),
                bill.getSgstTotal(),
                bill.getIgstTotal(),
                bill.getRoundOff(),
                bill.getGrandTotal(),
                bill.getPaymentStatus(),
                bill.getStatus(),
                bill.getIdempotencyKey(),
                bill.getReceiptUrl(),
                itemResponses,
                paymentResponses,
                bill.getCreatedAt()
        );
    }

    /** Resolve the current authenticated user's UUID from the security context. */
    private UUID resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof
                com.auradev.erp.auth.security.UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }
}
