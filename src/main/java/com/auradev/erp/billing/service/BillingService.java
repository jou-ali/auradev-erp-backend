package com.auradev.erp.billing.service;

import com.auradev.erp.audit.service.AuditService;
import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.billing.dto.*;
import com.auradev.erp.billing.entity.*;
import com.auradev.erp.billing.repository.BillRepository;
import com.auradev.erp.billing.repository.BillSequenceRepository;
import com.auradev.erp.billing.repository.CustomerRepository;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.util.MoneyUtils;
import com.auradev.erp.inventory.entity.Inventory;
import com.auradev.erp.inventory.entity.MovementType;
import com.auradev.erp.inventory.entity.RefType;
import com.auradev.erp.inventory.repository.InventoryRepository;
import com.auradev.erp.inventory.service.InventoryService;
import com.auradev.erp.settings.model.BillingConfig;
import com.auradev.erp.settings.model.GstScheme;
import com.auradev.erp.settings.model.TaxConfig;
import com.auradev.erp.settings.model.CategoryGstRate;
import com.auradev.erp.settings.service.SettingsService;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.repository.UserRepository;
import com.auradev.erp.common.pagination.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BillingService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final BillRepository billRepository;
    private final BillSequenceRepository sequenceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final AuditService auditService;
    private final SettingsService settingsService;

    @Transactional(readOnly = true)
    public List<CustomerResponse> listCustomers() {
        UUID tenantId = TenantContext.require();
        return customerRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    public BillResponse createBill(CreateBillRequest req) {
        UUID tenantId = TenantContext.require();
        UserPrincipal principal = requirePrincipal();
        UUID cashierId = principal.getId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));

        Customer customer = resolveCustomer(tenantId, req.customerId());
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new EntityNotFoundException("User", cashierId));

        TaxConfig tax = effectiveTaxConfig(req.gstSchemeOverride());
        List<ComputedLine> lines = computeLines(req.items(), tax);
        Totals totals = computeTotals(lines, req.discountMode(), req.billDiscount(), tax);
        assertBillingRules(principal.getRole(), req.discountMode(), req.billDiscount(), req.payment(), lines, totals);
        assertStockForSale(tenantId, lines, null);
        String billNo = nextBillNo(tenant);

        Bill bill = buildBillEntity(
                tenantId, cashierId, tenant, customer, lines, totals,
                req.discountMode(), billNo, BillStatus.COMPLETED, paymentStatusFor(req.payment().method()),
                tax.scheme());

        addPayments(bill, req.payment(), totals.grandTotal());

        Bill saved = billRepository.save(bill);

        for (ComputedLine cl : lines) {
            inventoryService.recordMovement(
                    cl.product().getId(),
                    MovementType.sale,
                    cl.quantity(),
                    RefType.bill,
                    saved.getId(),
                    "Sale " + billNo);
        }

        if (req.payment().method() == PaymentMethod.CREDIT) {
            customer.setCreditBalance(customer.getCreditBalance().add(totals.grandTotal()));
            customerRepository.save(customer);
        }

        auditService.log("BILL_CREATED", "bill", saved.getId(), Map.of(
                "billNo", billNo,
                "grandTotal", totals.grandTotal(),
                "customer", customer.getName()));

        return toBillResponse(saved, customer, cashier, BillStatus.COMPLETED, req.discountMode(), req.payment());
    }

    public BillResponse holdBill(HoldBillRequest req) {
        UUID tenantId = TenantContext.require();
        UserPrincipal principal = requirePrincipal();
        BillingConfig billing = settingsService.getBillingConfig();
        if (!billing.allowHoldBill()) {
            throw new BusinessException("HOLD_DISABLED", "Bill hold is disabled in billing settings");
        }
        UUID cashierId = principal.getId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));

        Customer customer = resolveCustomer(tenantId, req.customerId());
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new EntityNotFoundException("User", cashierId));

        TaxConfig tax = effectiveTaxConfig(req.gstSchemeOverride());
        List<ComputedLine> lines = computeLines(req.items(), tax);
        Totals totals = computeTotals(lines, req.discountMode(), req.billDiscount(), tax);
        assertStockForSale(tenantId, lines, null);

        Bill bill = buildBillEntity(
                tenantId, cashierId, tenant, customer, lines, totals,
                req.discountMode(), nextParkRef(tenant), BillStatus.HELD, BillPaymentStatus.PARTIAL,
                tax.scheme());

        Bill saved = billRepository.save(bill);
        return toBillResponse(saved, customer, cashier, BillStatus.HELD, req.discountMode(), null);
    }

    @Transactional(readOnly = true)
    public List<HeldBillSummaryResponse> listHeldBills() {
        UUID tenantId = TenantContext.require();
        return billRepository.findByTenantIdAndStatusOrderByUpdatedAtDesc(tenantId, BillStatus.HELD).stream()
                .map(b -> {
                    Customer customer = customerRepository.findById(b.getCustomerId())
                            .orElseThrow(() -> new EntityNotFoundException("Customer", b.getCustomerId()));
                    return new HeldBillSummaryResponse(
                            b.getId(),
                            b.getBillNo(),
                            customer.getName(),
                            b.getItems().size(),
                            b.getGrandTotal(),
                            b.getUpdatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BillSummaryResponse> listCompletedBills(String q, Pageable pageable) {
        UUID tenantId = TenantContext.require();
        var page = billRepository.searchCompletedSummaries(
                tenantId, BillStatus.COMPLETED, blankToNull(q), pageable);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public BillResponse getBill(UUID id) {
        UUID tenantId = TenantContext.require();
        Bill bill = billRepository.findByIdAndTenantIdAndStatus(id, tenantId, BillStatus.COMPLETED)
                .orElseThrow(() -> new EntityNotFoundException("Bill", id));
        bill.getPayments().size();
        Customer customer = customerRepository.findById(bill.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", bill.getCustomerId()));
        User cashier = userRepository.findById(bill.getCashierId())
                .orElseThrow(() -> new EntityNotFoundException("User", bill.getCashierId()));
        return toBillResponse(bill, customer, cashier, BillStatus.COMPLETED, bill.getDiscountMode(), null);
    }

    @Transactional(readOnly = true)
    public BillResponse getHeldBill(UUID id) {
        UUID tenantId = TenantContext.require();
        Bill bill = billRepository.findByIdAndTenantIdAndStatus(id, tenantId, BillStatus.HELD)
                .orElseThrow(() -> new EntityNotFoundException("HeldBill", id));
        Customer customer = customerRepository.findById(bill.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer", bill.getCustomerId()));
        User cashier = userRepository.findById(bill.getCashierId())
                .orElseThrow(() -> new EntityNotFoundException("User", bill.getCashierId()));
        return toBillResponse(bill, customer, cashier, BillStatus.HELD, bill.getDiscountMode(), null);
    }

    public BillResponse completeHeldBill(UUID id, CompleteHeldBillRequest req) {
        UUID tenantId = TenantContext.require();
        UserPrincipal principal = requirePrincipal();
        UUID cashierId = principal.getId();

        Bill bill = billRepository.findByIdAndTenantIdAndStatus(id, tenantId, BillStatus.HELD)
                .orElseThrow(() -> new EntityNotFoundException("HeldBill", id));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant", tenantId));
        Customer customer = resolveCustomer(tenantId, req.customerId() != null ? req.customerId() : bill.getCustomerId());
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new EntityNotFoundException("User", cashierId));

        TaxConfig tax = effectiveTaxConfig(req.gstSchemeOverride());
        List<ComputedLine> lines = computeLines(req.items(), tax);
        Totals totals = computeTotals(lines, req.discountMode(), req.billDiscount(), tax);
        assertBillingRules(principal.getRole(), req.discountMode(), req.billDiscount(), req.payment(), lines, totals);
        assertStockForSale(tenantId, lines, id);
        String billNo = nextBillNo(tenant);

        bill.getItems().clear();
        bill.setCustomerId(customer.getId());
        bill.setBillNo(billNo);
        bill.setSubtotal(totals.subtotal());
        bill.setBillDiscount(totals.discount());
        bill.setDiscountMode(req.discountMode());
        bill.setCgstTotal(totals.cgst());
        bill.setSgstTotal(totals.sgst());
        bill.setGrandTotal(totals.grandTotal());
        bill.setGstScheme(tax.scheme().name());
        bill.setPaymentStatus(paymentStatusFor(req.payment().method()));
        bill.setStatus(BillStatus.COMPLETED);
        bill.setUpdatedBy(cashierId);

        for (ComputedLine cl : lines) {
            bill.getItems().add(toBillItem(bill, cl));
        }

        bill.getPayments().clear();
        addPayments(bill, req.payment(), totals.grandTotal());

        Bill saved = billRepository.save(bill);

        for (ComputedLine cl : lines) {
            inventoryService.recordMovement(
                    cl.product().getId(),
                    MovementType.sale,
                    cl.quantity(),
                    RefType.bill,
                    saved.getId(),
                    "Sale " + billNo);
        }

        if (req.payment().method() == PaymentMethod.CREDIT) {
            customer.setCreditBalance(customer.getCreditBalance().add(totals.grandTotal()));
            customerRepository.save(customer);
        }

        return toBillResponse(saved, customer, cashier, BillStatus.COMPLETED, req.discountMode(), req.payment());
    }

    public void discardHeldBill(UUID id) {
        UUID tenantId = TenantContext.require();
        UserPrincipal principal = requirePrincipal();

        Bill bill = billRepository.findByIdAndTenantIdAndStatus(id, tenantId, BillStatus.HELD)
                .orElseThrow(() -> new EntityNotFoundException("HeldBill", id));

        bill.setStatus(BillStatus.VOID);
        bill.setPaymentStatus(BillPaymentStatus.VOID);
        bill.setUpdatedBy(principal.getId());
        billRepository.save(bill);
    }

    private Bill buildBillEntity(
            UUID tenantId,
            UUID cashierId,
            Tenant tenant,
            Customer customer,
            List<ComputedLine> lines,
            Totals totals,
            DiscountMode discountMode,
            String billNo,
            BillStatus status,
            BillPaymentStatus paymentStatus,
            GstScheme gstScheme) {

        Bill bill = new Bill();
        bill.setTenantId(tenantId);
        bill.setBillNo(billNo);
        bill.setCustomerId(customer.getId());
        bill.setCashierId(cashierId);
        bill.setPlaceOfSupplyState(tenant.getStateCode());
        bill.setSubtotal(totals.subtotal());
        bill.setBillDiscount(totals.discount());
        bill.setDiscountMode(discountMode);
        bill.setCgstTotal(totals.cgst());
        bill.setSgstTotal(totals.sgst());
        bill.setIgstTotal(BigDecimal.ZERO);
        bill.setRoundOff(BigDecimal.ZERO);
        bill.setGrandTotal(totals.grandTotal());
        bill.setGstScheme(gstScheme.name());
        bill.setPaymentStatus(paymentStatus);
        bill.setStatus(status);
        bill.setCreatedBy(cashierId);
        bill.setUpdatedBy(cashierId);

        for (ComputedLine cl : lines) {
            bill.getItems().add(toBillItem(bill, cl));
        }
        return bill;
    }

    private BillItem toBillItem(Bill bill, ComputedLine cl) {
        BillItem item = new BillItem();
        item.setBill(bill);
        item.setProduct(cl.product());
        item.setProductNameSnapshot(cl.product().getName());
        item.setSkuSnapshot(cl.product().getSku());
        item.setQuantity(cl.quantity());
        item.setUnitPrice(cl.unitPrice());
        item.setLineDiscount(cl.lineDiscount());
        item.setTaxableValue(cl.taxable());
        item.setGstRate(cl.gstRate());
        item.setCgstAmount(cl.cgst());
        item.setSgstAmount(cl.sgst());
        item.setIgstAmount(BigDecimal.ZERO);
        item.setLineTotal(cl.lineTotal());
        return item;
    }

    private void assertBillingRules(
            UserRole role,
            DiscountMode discountMode,
            BigDecimal billDiscount,
            PaymentInput payment,
            List<ComputedLine> lines,
            Totals totals) {
        BillingConfig billing = settingsService.getBillingConfig();

        if (payment != null && payment.method() == PaymentMethod.CREDIT && !billing.allowCreditSales()) {
            throw new BusinessException("CREDIT_DISABLED", "Credit sales are disabled in billing settings");
        }

        for (ComputedLine line : lines) {
            if (line.lineDiscount().compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal gross = MoneyUtils.roundHalfUp2(line.unitPrice().multiply(line.quantity()));
            if (gross.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal pct = line.lineDiscount()
                    .multiply(new BigDecimal("100"))
                    .divide(gross, 2, java.math.RoundingMode.HALF_UP);
            if (pct.compareTo(new BigDecimal(billing.maxLineDiscountPercent())) > 0) {
                throw new BusinessException(
                        "LINE_DISCOUNT_EXCEEDED",
                        "Line discount exceeds the allowed maximum of " + billing.maxLineDiscountPercent() + "%");
            }
        }

        if (totals.discount().compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal billDiscountPct;
        if (discountMode == DiscountMode.PERCENT) {
            billDiscountPct = billDiscount != null ? billDiscount : BigDecimal.ZERO;
        } else if (totals.subtotal().compareTo(BigDecimal.ZERO) > 0) {
            billDiscountPct = totals.discount()
                    .multiply(new BigDecimal("100"))
                    .divide(totals.subtotal(), 2, java.math.RoundingMode.HALF_UP);
        } else {
            billDiscountPct = BigDecimal.ZERO;
        }

        int allowed = role == UserRole.CASHIER
                ? billing.cashierMaxBillDiscountPercent()
                : billing.maxBillDiscountPercent();
        if (billDiscountPct.compareTo(new BigDecimal(allowed)) > 0) {
            throw new BusinessException(
                    "BILL_DISCOUNT_EXCEEDED",
                    "Bill discount exceeds the allowed maximum of " + allowed + "% for your role");
        }
    }

    private Customer resolveCustomer(UUID tenantId, UUID customerId) {
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new EntityNotFoundException("Customer", customerId));
            if (!tenantId.equals(customer.getTenantId())) {
                throw new BusinessException("CUSTOMER_FORBIDDEN", "Customer does not belong to this tenant");
            }
            return customer;
        }
        return customerRepository.findByTenantIdAndType(tenantId, CustomerType.walkin)
                .orElseThrow(() -> new BusinessException(
                        "NO_WALKIN_CUSTOMER",
                        "Walk-in customer not configured for this tenant"));
    }

    private List<ComputedLine> computeLines(List<BillLineRequest> items, TaxConfig tax) {
        List<ComputedLine> result = new ArrayList<>();
        for (BillLineRequest line : items) {
            Product product = productRepository.findWithCategoryById(line.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Product", line.productId()));
            if (!product.isActive()) {
                throw new BusinessException("PRODUCT_INACTIVE", "Product is inactive: " + product.getName());
            }

            BigDecimal qty = MoneyUtils.roundHalfUp3(line.quantity());
            BigDecimal unitPrice = product.getPriceSelling();
            BigDecimal lineDisc = MoneyUtils.roundHalfUp2(
                    line.lineDiscount() != null ? line.lineDiscount() : BigDecimal.ZERO);
            BigDecimal gross = MoneyUtils.roundHalfUp2(unitPrice.multiply(qty));
            BigDecimal netBeforeTax = MoneyUtils.roundHalfUp2(gross.subtract(lineDisc));

            if (tax.scheme() == GstScheme.COMPOSITE) {
                result.add(new ComputedLine(
                        product, qty, unitPrice, lineDisc, netBeforeTax,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, netBeforeTax));
                continue;
            }

            BigDecimal gstRate = resolveGstRate(product, tax);
            BigDecimal taxable;
            BigDecimal gst;
            if (tax.priceIncludesTax()) {
                BigDecimal divisor = BigDecimal.ONE.add(
                        gstRate.divide(new BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP));
                taxable = MoneyUtils.roundHalfUp2(netBeforeTax.divide(divisor, 2, java.math.RoundingMode.HALF_UP));
                gst = MoneyUtils.roundHalfUp2(netBeforeTax.subtract(taxable));
            } else {
                taxable = netBeforeTax;
                gst = MoneyUtils.pct(gross, gstRate);
            }
            BigDecimal halfGst = MoneyUtils.roundHalfUp2(gst.divide(new BigDecimal("2")));
            BigDecimal lineTotal = tax.priceIncludesTax()
                    ? netBeforeTax
                    : MoneyUtils.roundHalfUp2(gross.add(gst).subtract(lineDisc));

            result.add(new ComputedLine(
                    product, qty, unitPrice, lineDisc, taxable,
                    gstRate, halfGst, halfGst, lineTotal));
        }
        return result;
    }

    private BigDecimal lineNetTotal(List<ComputedLine> lines) {
        return lines.stream()
                .map(l -> MoneyUtils.roundHalfUp2(
                        l.unitPrice().multiply(l.quantity()).subtract(l.lineDiscount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal compositeGstOnBase(BigDecimal taxableBase, TaxConfig tax) {
        if (taxableBase.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (tax.priceIncludesTax()) {
            BigDecimal divisor = BigDecimal.ONE.add(
                    tax.compositeRatePct().divide(new BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP));
            BigDecimal taxable = MoneyUtils.roundHalfUp2(
                    taxableBase.divide(divisor, 2, java.math.RoundingMode.HALF_UP));
            return MoneyUtils.roundHalfUp2(taxableBase.subtract(taxable));
        }
        return MoneyUtils.pct(taxableBase, tax.compositeRatePct());
    }

    private TaxConfig effectiveTaxConfig(GstScheme override) {
        TaxConfig base = settingsService.getTaxConfig().normalized();
        if (override == null || override == base.scheme()) {
            return base;
        }
        return new TaxConfig(
                override,
                base.priceIncludesTax(),
                base.enabledRates(),
                base.compositeRatePct(),
                base.defaultCategoryRatePct(),
                base.categoryRates());
    }

    private BigDecimal resolveGstRate(Product product, TaxConfig tax) {
        return switch (tax.scheme()) {
            case COMPOSITE -> tax.compositeRatePct();
            case CATEGORY -> {
                Category category = product.getCategory();
                if (category != null) {
                    UUID categoryId = category.getId();
                    yield tax.categoryRates().stream()
                            .filter(r -> categoryId.equals(r.categoryId()))
                            .map(CategoryGstRate::ratePct)
                            .findFirst()
                            .orElse(tax.defaultCategoryRatePct());
                }
                yield tax.defaultCategoryRatePct();
            }
            default -> product.getTaxRatePct();
        };
    }

    private Totals computeTotals(List<ComputedLine> lines, DiscountMode mode, BigDecimal billDiscount, TaxConfig tax) {
        BigDecimal subtotal = lines.stream()
                .map(l -> MoneyUtils.roundHalfUp2(l.unitPrice().multiply(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = billDiscount != null ? billDiscount : BigDecimal.ZERO;
        if (mode == DiscountMode.PERCENT) {
            discount = MoneyUtils.pct(subtotal, discount);
        } else {
            discount = MoneyUtils.roundHalfUp2(discount);
        }

        BigDecimal gstTotal;
        BigDecimal grand;
        if (tax.scheme() == GstScheme.COMPOSITE) {
            BigDecimal taxableBase = MoneyUtils.roundHalfUp2(lineNetTotal(lines).subtract(discount));
            if (taxableBase.compareTo(BigDecimal.ZERO) < 0) {
                taxableBase = BigDecimal.ZERO;
            }
            gstTotal = compositeGstOnBase(taxableBase, tax);
            if (tax.priceIncludesTax()) {
                grand = taxableBase;
            } else {
                grand = MoneyUtils.roundHalfUp2(taxableBase.add(gstTotal));
            }
        } else {
            gstTotal = lines.stream()
                    .map(l -> {
                        BigDecimal gross = MoneyUtils.roundHalfUp2(l.unitPrice().multiply(l.quantity()));
                        if (tax.priceIncludesTax()) {
                            return MoneyUtils.roundHalfUp2(gross.subtract(l.lineDiscount()).subtract(l.taxable()));
                        }
                        return MoneyUtils.pct(gross, l.gstRate());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            grand = MoneyUtils.roundHalfUp2(subtotal.subtract(discount).add(gstTotal));
        }

        BigDecimal halfGst = MoneyUtils.roundHalfUp2(gstTotal.divide(new BigDecimal("2")));

        return new Totals(subtotal, discount, halfGst, halfGst, grand);
    }

    private String nextBillNo(Tenant tenant) {
        String fy = String.valueOf(Instant.now().atZone(IST).getYear());
        BillSequenceId id = new BillSequenceId(tenant.getId(), fy);
        BillSequence seq = sequenceRepository.findForUpdate(tenant.getId(), fy)
                .orElseGet(() -> {
                    BillSequence created = new BillSequence();
                    created.setId(id);
                    created.setLastNo(0);
                    return created;
                });
        seq.setLastNo(seq.getLastNo() + 1);
        sequenceRepository.save(seq);
        return tenant.getBillNoPrefix() + "-" + fy + "-" + String.format("%05d", seq.getLastNo());
    }

    private String nextParkRef(Tenant tenant) {
        // bill_sequences.fy is VARCHAR(4) — use a fixed bucket key, not "PARK-2026"
        String fy = "HOLD";
        BillSequenceId id = new BillSequenceId(tenant.getId(), fy);
        BillSequence seq = sequenceRepository.findForUpdate(tenant.getId(), fy)
                .orElseGet(() -> {
                    BillSequence created = new BillSequence();
                    created.setId(id);
                    created.setLastNo(0);
                    return created;
                });
        seq.setLastNo(seq.getLastNo() + 1);
        sequenceRepository.save(seq);
        String year = String.valueOf(Instant.now().atZone(IST).getYear());
        return "PARK-" + year + "-" + String.format("%05d", seq.getLastNo());
    }

    private void assertStockForSale(UUID tenantId, List<ComputedLine> lines, UUID excludeHeldBillId) {
        if (lines.isEmpty()) {
            return;
        }

        Map<UUID, BigDecimal> needed = new HashMap<>();
        Map<UUID, Product> products = new HashMap<>();
        for (ComputedLine line : lines) {
            needed.merge(line.product().getId(), line.quantity(), BigDecimal::add);
            products.putIfAbsent(line.product().getId(), line.product());
        }

        Map<UUID, BigDecimal> heldByProduct = billRepository.sumHeldQuantitiesByProduct(tenantId, excludeHeldBillId);

        for (Map.Entry<UUID, BigDecimal> entry : needed.entrySet()) {
            UUID productId = entry.getKey();
            BigDecimal qtyNeeded = entry.getValue();
            Product product = products.get(productId);

            Inventory inv = inventoryRepository.findByTenantIdAndProductId(tenantId, productId).orElse(null);
            BigDecimal onHand = inv != null ? inv.getQuantityOnHand() : BigDecimal.ZERO;
            BigDecimal held = heldByProduct.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal available = onHand.subtract(held);

            if (available.compareTo(qtyNeeded) < 0) {
                String unit = product.getUnitLabel();
                String heldNote = held.compareTo(BigDecimal.ZERO) > 0
                        ? " (" + held.stripTrailingZeros().toPlainString() + " " + unit + " in parked bills)"
                        : "";
                throw new BusinessException(
                        "INSUFFICIENT_STOCK",
                        product.getName() + " — only "
                                + available.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
                                + " " + unit + " available"
                                + heldNote);
            }
        }
    }

    private void addPayments(Bill bill, PaymentInput input, BigDecimal grandTotal) {
        BigDecimal splitCash = input.splitCashAmount();
        if (splitCash != null
                && splitCash.compareTo(BigDecimal.ZERO) > 0
                && splitCash.compareTo(grandTotal) < 0) {
            BigDecimal cash = MoneyUtils.roundHalfUp2(splitCash);
            BigDecimal upi = MoneyUtils.roundHalfUp2(grandTotal.subtract(cash));

            Payment cashPay = new Payment();
            cashPay.setBill(bill);
            cashPay.setMethod(PaymentMethod.CASH);
            cashPay.setAmount(cash);
            bill.getPayments().add(cashPay);

            Payment upiPay = new Payment();
            upiPay.setBill(bill);
            upiPay.setMethod(PaymentMethod.UPI);
            upiPay.setAmount(upi);
            bill.getPayments().add(upiPay);
            return;
        }

        PaymentMethod method = input.method();
        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setMethod(method);
        payment.setAmount(grandTotal);

        if (method == PaymentMethod.CASH && input.tendered() != null) {
            payment.setTendered(MoneyUtils.roundHalfUp2(input.tendered()));
            payment.setChangeDue(MoneyUtils.roundHalfUp2(input.tendered().subtract(grandTotal).max(BigDecimal.ZERO)));
        }
        bill.getPayments().add(payment);
    }

    private BillPaymentStatus paymentStatusFor(PaymentMethod method) {
        return method == PaymentMethod.CREDIT ? BillPaymentStatus.CREDIT : BillPaymentStatus.PAID;
    }

    private BillResponse toBillResponse(
            Bill bill,
            Customer customer,
            User cashier,
            BillStatus status,
            DiscountMode discountMode,
            PaymentInput payment) {
        List<BillLineResponse> lines = bill.getItems().stream()
                .map(i -> new BillLineResponse(
                        i.getProduct().getId(),
                        i.getProductNameSnapshot(),
                        i.getSkuSnapshot(),
                        i.getProduct().getUnitLabel(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getLineDiscount(),
                        i.getGstRate(),
                        i.getLineTotal()))
                .toList();

        Payment primary = bill.getPayments().isEmpty() ? null : bill.getPayments().get(0);
        PaymentMethod method = null;
        if (payment != null) {
            boolean isSplit = payment.splitCashAmount() != null
                    && payment.splitCashAmount().compareTo(BigDecimal.ZERO) > 0
                    && bill.getPayments().size() > 1;
            method = isSplit ? PaymentMethod.CASH : payment.method();
        } else if (primary != null) {
            method = primary.getMethod();
        }

        return new BillResponse(
                bill.getId(),
                bill.getBillNo(),
                customer.getId(),
                customer.getName(),
                cashier.getName(),
                status,
                discountMode,
                bill.getGstScheme() != null ? bill.getGstScheme() : GstScheme.PRODUCT.name(),
                bill.getSubtotal(),
                bill.getBillDiscount(),
                bill.getCgstTotal(),
                bill.getSgstTotal(),
                bill.getGrandTotal(),
                bill.getPaymentStatus(),
                method,
                primary != null ? primary.getTendered() : null,
                primary != null ? primary.getChangeDue() : null,
                bill.getCreatedAt(),
                bill.getUpdatedAt(),
                computeGstSlabs(bill),
                lines);
    }

    private List<GstSlabSummary> computeGstSlabs(Bill bill) {
        if (GstScheme.COMPOSITE.name().equals(bill.getGstScheme())) {
            BigDecimal tax = bill.getCgstTotal().add(bill.getSgstTotal()).add(bill.getIgstTotal());
            BigDecimal taxable = bill.getItems().stream()
                    .map(BillItem::getTaxableValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .subtract(bill.getBillDiscount());
            if (taxable.compareTo(BigDecimal.ZERO) < 0) {
                taxable = BigDecimal.ZERO;
            }
            BigDecimal rate = taxable.compareTo(BigDecimal.ZERO) > 0
                    ? tax.multiply(new BigDecimal("100"))
                            .divide(taxable, 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return List.of(new GstSlabSummary(rate, taxable, tax));
        }
        Map<BigDecimal, BigDecimal[]> byRate = new TreeMap<>();
        for (BillItem item : bill.getItems()) {
            BigDecimal rate = item.getGstRate().stripTrailingZeros();
            BigDecimal[] acc = byRate.computeIfAbsent(rate, k -> new BigDecimal[]{
                    BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(item.getTaxableValue());
            BigDecimal itemTax = item.getCgstAmount().add(item.getSgstAmount()).add(item.getIgstAmount());
            acc[1] = acc[1].add(itemTax);
        }
        return byRate.entrySet().stream()
                .map(e -> new GstSlabSummary(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    private CustomerResponse toCustomerResponse(Customer c) {
        return new CustomerResponse(
                c.getId(), c.getName(), c.getPhone(), c.getType(),
                c.getLoyaltyPoints(), c.getCreditBalance());
    }

    private UserPrincipal requirePrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        throw new BusinessException("UNAUTHENTICATED", "User context required");
    }

    private static String blankToNull(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }

    private record ComputedLine(
            Product product,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineDiscount,
            BigDecimal taxable,
            BigDecimal gstRate,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal lineTotal) {}

    private record Totals(
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal grandTotal) {}
}
