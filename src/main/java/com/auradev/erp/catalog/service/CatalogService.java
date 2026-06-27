package com.auradev.erp.catalog.service;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.catalog.dto.*;
import com.auradev.erp.catalog.entity.*;
import com.auradev.erp.catalog.repository.CategoryRepository;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.catalog.repository.SupplierRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.dashboard.repository.DashboardQueryRepository;
import com.auradev.erp.inventory.entity.Inventory;
import com.auradev.erp.inventory.entity.MovementType;
import com.auradev.erp.inventory.repository.InventoryRepository;
import com.auradev.erp.inventory.service.InventoryService;
import com.auradev.erp.tenant.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.auradev.erp.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final DashboardQueryRepository dashboardQueryRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(
            UUID tenantId, String q, String category, String unit, String status, Pageable pageable) {

        Page<Product> page = (q != null && !q.isBlank())
                ? productRepository.searchActive(q.trim(), pageable)
                : productRepository.findByActiveTrue(pageable);

        List<Product> products = page.getContent();
        Map<UUID, Inventory> inventoryByProduct = loadInventoryMap(tenantId, products);

        List<ProductResponse> items = products.stream()
                .filter(p -> matchesCategory(p, category))
                .filter(p -> matchesUnit(p, unit))
                .map(p -> toResponse(p, inventoryByProduct.get(p.getId()), isCashier()))
                .filter(r -> matchesStatus(r, status))
                .collect(Collectors.toList());

        return new PageResponse<>(items, page.getNumber(), page.getSize(), items.size(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
        UUID tenantId = TenantContext.require();
        return toResponse(product, inventoryService.getInventory(tenantId, id), isCashier());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listPosQuickPicks(UUID tenantId, int limit) {
        int cap = Math.min(Math.max(limit, 1), 24);
        List<UUID> topIds = dashboardQueryRepository.topSellingProductIds(tenantId, cap, 7);
        List<Product> products = new ArrayList<>();

        if (!topIds.isEmpty()) {
            Map<UUID, Product> byId = productRepository.findAllById(topIds).stream()
                    .filter(Product::isActive)
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            topIds.stream().map(byId::get).filter(p -> p != null).forEach(products::add);
        }

        if (products.size() < cap) {
            Set<UUID> seen = products.stream().map(Product::getId).collect(Collectors.toCollection(HashSet::new));
            productRepository.findByActiveTrue(PageRequest.of(0, cap, Sort.by("name"))).getContent().stream()
                    .filter(p -> !seen.contains(p.getId()))
                    .limit(cap - products.size())
                    .forEach(products::add);
        }

        Map<UUID, Inventory> inventoryByProduct = loadInventoryMap(tenantId, products);
        boolean cashier = isCashier();
        return products.stream()
                .map(p -> toResponse(p, inventoryByProduct.get(p.getId()), cashier))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new EntityNotFoundException("Product[barcode=" + barcode + "]", barcode));
        UUID tenantId = TenantContext.require();
        return toResponse(product, inventoryService.getInventory(tenantId, product.getId()), isCashier());
    }

    public ProductResponse createProduct(CreateProductRequest req) {
        UUID tenantId = TenantContext.require();
        validateSkuUnique(req.sku(), null);
        if (req.barcode() != null && !req.barcode().isBlank()) {
            validateBarcodeUnique(req.barcode(), null);
        }

        Product product = new Product();
        applyCreate(product, req);
        Product saved = productRepository.save(product);

        inventoryService.ensureInventoryRow(
                tenantId, saved.getId(), req.initialStock(),
                req.lowStockThreshold(), req.reorderQuantity());

        log.info("Product created id={} sku={}", saved.getId(), saved.getSku());
        return toResponse(saved, inventoryService.getInventory(tenantId, saved.getId()), isCashier());
    }

    public ProductResponse updateProduct(UUID id, UpdateProductRequest req) {
        UUID tenantId = TenantContext.require();
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));

        validateSkuUnique(req.sku(), id);
        if (req.barcode() != null && !req.barcode().isBlank()) {
            validateBarcodeUnique(req.barcode(), id);
        }

        applyUpdate(product, req);
        Product saved = productRepository.save(product);
        inventoryService.updateThresholds(tenantId, id, req.lowStockThreshold(), req.reorderQuantity());

        return toResponse(saved, inventoryService.getInventory(tenantId, saved.getId()), isCashier());
    }

    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
        if (inventoryService.hasMovements(id)) {
            throw new BusinessException("PRODUCT_HAS_MOVEMENTS",
                    "Cannot deactivate product with movement history until stock is zeroed");
        }
        product.setActive(false);
        productRepository.save(product);
    }

    public void adjustStock(UUID productId, MovementType movementType, BigDecimal quantity, String notes) {
        inventoryService.recordMovement(productId, movementType, quantity, null, null, notes);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID tenantId, String q, String category, String unit, String status) {
        PageResponse<ProductResponse> page = listProducts(tenantId, q, category, unit, status, Pageable.unpaged());
        boolean stripCost = isCashier();
        String[] headers = stripCost
                ? new String[]{
                        "ID", "Name", "SKU", "Barcode", "Category", "Unit",
                        "MRP", "Selling", "Tax%", "Stock", "Reorder", "Status",
                        "Selling Value", "MRP Value"
                }
                : new String[]{
                        "ID", "Name", "SKU", "Barcode", "Category", "Unit",
                        "MRP", "Selling", "Cost", "Tax%", "Stock", "Reorder", "Status",
                        "Cost Value", "Selling Value", "MRP Value"
                };

        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (ProductResponse p : page.content()) {
                BigDecimal qty = p.quantityOnHand() != null ? p.quantityOnHand() : BigDecimal.ZERO;
                BigDecimal sellingValue = qty.multiply(p.priceSelling() != null ? p.priceSelling() : BigDecimal.ZERO);
                BigDecimal mrpValue = qty.multiply(p.priceMrp() != null ? p.priceMrp() : BigDecimal.ZERO);
                if (stripCost) {
                    printer.printRecord(p.id(), p.name(), p.sku(), p.barcode(), p.categoryName(),
                            p.unitLabel(), p.priceMrp(), p.priceSelling(), p.taxRatePct(),
                            p.quantityOnHand(), p.reorderQuantity(), p.stockStatus(),
                            sellingValue, mrpValue);
                } else {
                    BigDecimal cost = p.costPrice() != null ? p.costPrice() : BigDecimal.ZERO;
                    BigDecimal costValue = qty.multiply(cost);
                    printer.printRecord(p.id(), p.name(), p.sku(), p.barcode(), p.categoryName(),
                            p.unitLabel(), p.priceMrp(), p.priceSelling(), p.costPrice(), p.taxRatePct(),
                            p.quantityOnHand(), p.reorderQuantity(), p.stockStatus(),
                            costValue, sellingValue, mrpValue);
                }
            }
        } catch (IOException e) {
            throw new BusinessException("CSV_EXPORT_FAILED", e.getMessage());
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toCategoryResponse).collect(Collectors.toList());
    }

    public CategoryResponse createCategory(CreateCategoryRequest req) {
        String slug = resolveSlug(req.name(), req.slug());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("DUPLICATE_CATEGORY", "Category slug already exists: " + slug);
        }
        Category category = new Category();
        category.setName(req.name().trim());
        category.setSlug(slug);
        category.setParent(resolveParent(req.parentId()));
        category.setActive(req.isActive() == null || req.isActive());
        return toCategoryResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(UUID id, CreateCategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
        String slug = resolveSlug(req.name(), req.slug());
        categoryRepository.findBySlug(slug).filter(c -> !c.getId().equals(id)).ifPresent(c -> {
            throw new BusinessException("DUPLICATE_CATEGORY", "Category slug already exists: " + slug);
        });
        category.setName(req.name().trim());
        category.setSlug(slug);
        category.setParent(resolveParent(req.parentId()));
        if (req.isActive() != null) category.setActive(req.isActive());
        return toCategoryResponse(categoryRepository.save(category));
    }

    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
        category.setActive(false);
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listSuppliers() {
        return supplierRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toSupplierResponse)
                .collect(Collectors.toList());
    }

    public SupplierResponse createSupplier(CreateSupplierRequest req) {
        String name = req.name().trim();
        if (supplierRepository.existsByNameIgnoreCaseAndActiveTrue(name)) {
            throw new BusinessException("DUPLICATE_SUPPLIER", "Supplier already exists: " + name);
        }
        String gstin = blankToNull(req.gstin());
        if (gstin != null) {
            supplierRepository.findByGstinIgnoreCaseAndActiveTrue(gstin).ifPresent(s -> {
                throw new BusinessException("DUPLICATE_GSTIN", "GSTIN already used by: " + s.getName());
            });
        }
        Supplier supplier = new Supplier();
        applySupplierFields(supplier, req);
        supplier.setActive(true);
        return toSupplierResponse(supplierRepository.save(supplier));
    }

    public SupplierResponse updateSupplier(UUID id, CreateSupplierRequest req) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier", id));
        String name = req.name().trim();
        supplierRepository.findByNameIgnoreCaseAndActiveTrue(name)
                .filter(s -> !s.getId().equals(id))
                .ifPresent(s -> {
                    throw new BusinessException("DUPLICATE_SUPPLIER", "Supplier already exists: " + name);
                });
        String gstin = blankToNull(req.gstin());
        if (gstin != null) {
            supplierRepository.findByGstinIgnoreCaseAndActiveTrue(gstin)
                    .filter(s -> !s.getId().equals(id))
                    .ifPresent(s -> {
                        throw new BusinessException("DUPLICATE_GSTIN", "GSTIN already used by: " + s.getName());
                    });
        }
        applySupplierFields(supplier, req);
        return toSupplierResponse(supplierRepository.save(supplier));
    }

    public void deleteSupplier(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier", id));
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    private void applySupplierFields(Supplier supplier, CreateSupplierRequest req) {
        supplier.setName(req.name().trim());
        supplier.setContactPerson(blankToNull(req.contactPerson()));
        supplier.setPhone(blankToNull(req.phone()));
        supplier.setEmail(blankToNull(req.email()));
        supplier.setGstin(blankToNull(req.gstin()));
        supplier.setAddress(blankToNull(req.address()));
    }

    private SupplierResponse toSupplierResponse(Supplier s) {
        return new SupplierResponse(
                s.getId(),
                s.getName(),
                s.getContactPerson(),
                s.getPhone(),
                s.getEmail(),
                s.getGstin(),
                s.getAddress());
    }

    private void applyCreate(Product product, CreateProductRequest req) {
        product.setName(req.name().trim());
        product.setSku(req.sku().trim());
        product.setBarcode(blankToNull(req.barcode()));
        product.setCategory(resolveCategory(req.categoryId()));
        product.setSupplier(resolveSupplier(req.supplierId()));
        product.setUnitType(req.unitType() != null ? req.unitType() : UnitType.unit);
        product.setUnitLabel(req.unitLabel());
        product.setPriceMrp(req.priceMrp());
        product.setPriceSelling(req.priceSelling());
        product.setCostPrice(req.costPrice());
        product.setTaxRatePct(req.taxRatePct());
        product.setActive(true);
    }

    private void applyUpdate(Product product, UpdateProductRequest req) {
        product.setName(req.name().trim());
        product.setSku(req.sku().trim());
        product.setBarcode(blankToNull(req.barcode()));
        product.setCategory(resolveCategory(req.categoryId()));
        product.setSupplier(resolveSupplier(req.supplierId()));
        product.setUnitType(req.unitType() != null ? req.unitType() : UnitType.unit);
        product.setUnitLabel(req.unitLabel());
        product.setPriceMrp(req.priceMrp());
        product.setPriceSelling(req.priceSelling());
        product.setCostPrice(req.costPrice());
        product.setTaxRatePct(req.taxRatePct());
    }

    private ProductResponse toResponse(Product p, Inventory inv, boolean stripCost) {
        BigDecimal qty = inv != null ? inv.getQuantityOnHand() : BigDecimal.ZERO;
        BigDecimal low = inv != null ? inv.getLowStockThreshold() : null;
        BigDecimal reorder = inv != null ? inv.getReorderQuantity() : null;
        return new ProductResponse(
                p.getId(), p.getName(), p.getSku(), p.getBarcode(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getUnitLabel(), p.getUnitType(),
                p.getPriceMrp(), p.getPriceSelling(),
                stripCost ? null : p.getCostPrice(), p.getTaxRatePct(),
                qty, low, reorder,
                p.stockStatus(qty, low), p.isActive(), p.getCreatedAt()
        );
    }

    private Map<UUID, Inventory> loadInventoryMap(UUID tenantId, List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = products.stream().map(Product::getId).toList();
        return inventoryRepository.findByTenantIdAndProductIdIn(tenantId, ids).stream()
                .collect(Collectors.toMap(inv -> inv.getProduct().getId(), Function.identity()));
    }

    private CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(
                c.getId(), c.getName(), c.getSlug(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.isActive(), c.getCreatedAt());
    }

    private Category resolveCategory(String categoryId) {
        UUID id = UUID.fromString(categoryId);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
    }

    private Supplier resolveSupplier(String supplierId) {
        if (supplierId == null || supplierId.isBlank()) return null;
        UUID id = UUID.fromString(supplierId);
        return supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier", id));
    }

    private Category resolveParent(String parentId) {
        if (parentId == null || parentId.isBlank()) return null;
        UUID id = UUID.fromString(parentId);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
    }

    private void validateSkuUnique(String sku, UUID excludeId) {
        productRepository.findBySku(sku).filter(p -> !p.getId().equals(excludeId)).ifPresent(p -> {
            throw new BusinessException("DUPLICATE_SKU", "SKU already in use: " + sku);
        });
    }

    private void validateBarcodeUnique(String barcode, UUID excludeId) {
        productRepository.findByBarcode(barcode).filter(p -> !p.getId().equals(excludeId)).ifPresent(p -> {
            throw new BusinessException("DUPLICATE_BARCODE", "Barcode already in use: " + barcode);
        });
    }

    private String resolveSlug(String name, String slug) {
        if (slug != null && !slug.isBlank()) return slug.trim().toLowerCase(Locale.ROOT);
        return name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private boolean isCashier() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal up
                && up.getRole() == UserRole.CASHIER;
    }

    private boolean matchesCategory(Product p, String category) {
        if (category == null || category.isBlank()) return true;
        return p.getCategory() != null && category.equalsIgnoreCase(p.getCategory().getName());
    }

    private boolean matchesUnit(Product p, String unit) {
        if (unit == null || unit.isBlank()) return true;
        return p.getUnitLabel().equalsIgnoreCase(unit)
                || p.getUnitType().name().equalsIgnoreCase(unit);
    }

    private boolean matchesStatus(ProductResponse r, String status) {
        if (status == null || status.isBlank()) return true;
        return switch (status.toLowerCase()) {
            case "in" -> r.stockStatus() == StockStatus.IN;
            case "low" -> r.stockStatus() == StockStatus.LOW;
            case "out" -> r.stockStatus() == StockStatus.OUT;
            case "active" -> r.isActive();
            case "inactive" -> !r.isActive();
            default -> true;
        };
    }
}
