package com.auradev.erp.catalog.service;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.catalog.dto.CategoryResponse;
import com.auradev.erp.catalog.dto.CreateCategoryRequest;
import com.auradev.erp.catalog.dto.CreateProductRequest;
import com.auradev.erp.catalog.dto.ProductListResponse;
import com.auradev.erp.catalog.dto.ProductResponse;
import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.entity.ProductUnit;
import com.auradev.erp.catalog.entity.StockStatus;
import com.auradev.erp.catalog.repository.CategoryRepository;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.common.error.BusinessException;
import com.auradev.erp.common.error.EntityNotFoundException;
import com.auradev.erp.common.pagination.PageResponse;
import com.auradev.erp.inventory.entity.MovementReason;
import com.auradev.erp.inventory.service.InventoryService;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for catalog management.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>CRUD on {@link Product} with per-tenant SKU/barcode uniqueness checks.</li>
 *   <li>Server-side filtering (name/sku/barcode, category, unit, stock status).</li>
 *   <li>Role-based field redaction: {@code costPrice} is stripped for
 *       {@link UserRole#CASHIER} callers.</li>
 *   <li>Delegation to {@link InventoryService} for all stock mutations.</li>
 *   <li>CSV export via Apache Commons CSV
 *       (add {@code commons-csv:1.11.0} to {@code pom.xml}).</li>
 * </ul>
 *
 * <p><strong>pom.xml dependency required:</strong></p>
 * <pre>{@code
 * <dependency>
 *   <groupId>org.apache.commons</groupId>
 *   <artifactId>commons-csv</artifactId>
 *   <version>1.11.0</version>
 * </dependency>
 * }</pre>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CatalogService {

    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryService   inventoryService;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Return a paginated, filtered list of products for the given tenant.
     *
     * @param tenantId  tenant to scope the query to
     * @param q         optional free-text search across name, SKU, and barcode
     * @param category  optional exact category name filter
     * @param unit      optional unit filter (matches {@link ProductUnit#name()})
     * @param status    optional status filter: {@code in}, {@code low}, {@code out},
     *                  {@code active}, {@code inactive}
     * @param pageable  page/sort parameters
     * @return paginated response with cost data stripped for CASHIER callers
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(
            UUID tenantId,
            String q,
            String category,
            String unit,
            String status,
            Pageable pageable) {

        boolean stripCost = isCashier();

        // Load the full page for this tenant, then apply in-memory filters.
        // NOTE: For very large catalogs (>50 k products) this should be
        // replaced with a JpaSpecificationExecutor-based approach that pushes
        // all predicates to the database.
        Page<Product> page = productRepository.findByTenantId(tenantId, pageable);

        List<ProductResponse> filtered = page.getContent().stream()
                .filter(p -> matchesSearch(p, q))
                .filter(p -> matchesCategory(p, category))
                .filter(p -> matchesUnit(p, unit))
                .filter(p -> matchesStatus(p, status))
                .map(p -> toResponse(p, stripCost))
                .collect(Collectors.toList());

        // Re-wrap as a PageResponse; element count may differ after in-memory
        // filtering so we report the filtered size as totalElements.
        return new PageResponse<>(
                filtered,
                page.getNumber(),
                page.getSize(),
                filtered.size(),
                page.getTotalPages()
        );
    }

    /**
     * Fetch a single product by ID, scoped to the caller's tenant.
     *
     * @param id product UUID
     * @return product response
     * @throws EntityNotFoundException if the product does not exist in the tenant
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        UUID tenantId = TenantContext.require();
        Product product = productRepository.findById(id)
                .filter(p -> tenantId.equals(p.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Product", id));
        return toResponse(product, isCashier());
    }

    /**
     * Look up a product by barcode for POS scan-to-cart.
     *
     * @param barcode the scanned barcode value
     * @return product response
     * @throws EntityNotFoundException if no product with that barcode exists
     */
    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        UUID tenantId = TenantContext.require();
        Product product = productRepository.findByTenantIdAndBarcode(tenantId, barcode)
                .orElseThrow(() -> new EntityNotFoundException("Product[barcode=" + barcode + "]", barcode));
        return toResponse(product, isCashier());
    }

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /**
     * Create a new product for the current tenant.
     *
     * @param req validated creation request
     * @return the persisted product as a response DTO
     * @throws BusinessException if the SKU or barcode already exists for this tenant
     */
    public ProductResponse createProduct(CreateProductRequest req) {
        UUID tenantId = TenantContext.require();
        UserPrincipal caller = currentPrincipal();

        validateSkuUnique(tenantId, req.sku(), null);
        if (req.sku() != null && req.barcode() != null && !req.barcode().isBlank()) {
            validateBarcodeUnique(tenantId, req.barcode(), null);
        }

        Category category = resolveCategory(tenantId, req.categoryId());

        Product product = new Product();
        product.setTenantId(tenantId);
        product.setCreatedBy(caller.getId());
        applyRequest(product, req, category);

        Product saved = productRepository.save(product);
        log.info("Product created: id={} sku={} tenant={}", saved.getId(), saved.getSku(), tenantId);
        return toResponse(saved, isCashier());
    }

    /**
     * Replace all mutable fields of an existing product.
     *
     * @param id  product UUID
     * @param req validated update request
     * @return the updated product as a response DTO
     * @throws EntityNotFoundException if the product does not exist in the tenant
     * @throws BusinessException       if the new SKU or barcode conflicts with another product
     */
    public ProductResponse updateProduct(UUID id, CreateProductRequest req) {
        UUID tenantId = TenantContext.require();
        UserPrincipal caller = currentPrincipal();

        Product product = productRepository.findById(id)
                .filter(p -> tenantId.equals(p.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Product", id));

        validateSkuUnique(tenantId, req.sku(), id);
        if (req.barcode() != null && !req.barcode().isBlank()) {
            validateBarcodeUnique(tenantId, req.barcode(), id);
        }

        Category category = resolveCategory(tenantId, req.categoryId());
        product.setUpdatedBy(caller.getId());
        applyRequest(product, req, category);

        Product saved = productRepository.save(product);
        log.info("Product updated: id={} sku={} tenant={}", saved.getId(), saved.getSku(), tenantId);
        return toResponse(saved, isCashier());
    }

    /**
     * Soft-delete a product by setting {@code active = false}.
     *
     * @param id product UUID
     * @throws EntityNotFoundException if the product does not exist in the tenant
     * @throws BusinessException       if the product has unprocessed stock movements
     *                                 that would leave inventory in an inconsistent state
     */
    public void deleteProduct(UUID id) {
        UUID tenantId = TenantContext.require();

        Product product = productRepository.findById(id)
                .filter(p -> tenantId.equals(p.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Product", id));

        // Reject hard-delete when any stock movements reference this product.
        // The caller must zero-out inventory via a COUNT_CORRECTION movement first.
        boolean hasMovements = inventoryService.hasMovements(id);
        if (hasMovements) {
            throw new BusinessException(
                    "PRODUCT_HAS_MOVEMENTS",
                    "Cannot delete product " + id + " because it has existing stock movements. " +
                    "Perform a COUNT_CORRECTION movement to zero the stock, then deactivate.");
        }

        product.setActive(false);
        productRepository.save(product);
        log.info("Product soft-deleted: id={} tenant={}", id, tenantId);
    }

    /**
     * Adjust product stock by delegating to the inventory service.
     *
     * @param productId product UUID
     * @param delta     positive = stock in, negative = stock out
     * @param reason    movement reason
     * @param notes     optional free-text note
     */
    public void adjustStock(UUID productId, BigDecimal delta, MovementReason reason, String notes) {
        inventoryService.recordMovement(productId, delta, reason, null, null, notes);
    }

    // -------------------------------------------------------------------------
    // CSV Export
    // -------------------------------------------------------------------------

    /**
     * Export the product catalog as a UTF-8 CSV byte array.
     *
     * <p>The {@code costPrice} column is omitted from the export when the
     * caller's role does not grant access to cost information.</p>
     *
     * @param tenantId tenant to export
     * @param q        optional free-text filter (same semantics as
     *                 {@link #listProducts})
     * @param category optional category name filter
     * @param unit     optional unit filter
     * @param status   optional status filter
     * @return UTF-8 encoded CSV bytes ready to stream as a download response
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID tenantId, String q, String category, String unit, String status) {
        boolean stripCost = isCashier();

        List<Product> products = productRepository.findByTenantId(tenantId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(p -> matchesSearch(p, q))
                .filter(p -> matchesCategory(p, category))
                .filter(p -> matchesUnit(p, unit))
                .filter(p -> matchesStatus(p, status))
                .collect(Collectors.toList());

        String[] headers = stripCost
                ? new String[]{"ID", "Name", "SKU", "Barcode", "Category", "Unit",
                               "MRP", "Selling Price", "GST Rate", "HSN Code",
                               "Current Stock", "Reorder Level", "Stock Status", "Active"}
                : new String[]{"ID", "Name", "SKU", "Barcode", "Category", "Unit",
                               "MRP", "Selling Price", "Cost Price", "GST Rate", "HSN Code",
                               "Current Stock", "Reorder Level", "Stock Status", "Active"};

        StringWriter sw = new StringWriter();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();

        try (CSVPrinter printer = new CSVPrinter(sw, format)) {
            for (Product p : products) {
                if (stripCost) {
                    printer.printRecord(
                            p.getId(), p.getName(), p.getSku(), p.getBarcode(),
                            p.getCategory() != null ? p.getCategory().getName() : "",
                            p.getUnit(), p.getMrp(), p.getSellingPrice(),
                            p.getGstRate(), p.getHsnCode(),
                            p.getCurrentStock(), p.getReorderLevel(),
                            p.stockStatus(), p.isActive()
                    );
                } else {
                    printer.printRecord(
                            p.getId(), p.getName(), p.getSku(), p.getBarcode(),
                            p.getCategory() != null ? p.getCategory().getName() : "",
                            p.getUnit(), p.getMrp(), p.getSellingPrice(), p.getCostPrice(),
                            p.getGstRate(), p.getHsnCode(),
                            p.getCurrentStock(), p.getReorderLevel(),
                            p.stockStatus(), p.isActive()
                    );
                }
            }
        } catch (IOException e) {
            throw new BusinessException("CSV_EXPORT_FAILED", "Failed to generate CSV export: " + e.getMessage());
        }

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void applyRequest(Product product, CreateProductRequest req, Category category) {
        product.setCategory(category);
        product.setName(req.name());
        product.setSku(req.sku());
        product.setBarcode(req.barcode() != null && req.barcode().isBlank() ? null : req.barcode());
        product.setUnit(req.unit() != null ? req.unit() : ProductUnit.PCS);
        product.setMrp(req.mrp());
        product.setSellingPrice(req.sellingPrice());
        product.setCostPrice(req.costPrice());
        product.setGstRate(req.gstRate());
        product.setHsnCode(req.hsnCode());
        product.setCurrentStock(req.currentStock());
        product.setReorderLevel(req.reorderLevel());
    }

    private Category resolveCategory(UUID tenantId, String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        UUID catUuid;
        try {
            catUuid = UUID.fromString(categoryId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_CATEGORY_ID", "Invalid category ID format: " + categoryId);
        }
        return categoryRepository.findById(catUuid)
                .filter(c -> tenantId.equals(c.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Category", catUuid));
    }

    private void validateSkuUnique(UUID tenantId, String sku, UUID excludeId) {
        productRepository.findByTenantIdAndSku(tenantId, sku)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "DUPLICATE_SKU",
                            "SKU '" + sku + "' is already used by another product in this tenant");
                });
    }

    private void validateBarcodeUnique(UUID tenantId, String barcode, UUID excludeId) {
        productRepository.findByTenantIdAndBarcode(tenantId, barcode)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "DUPLICATE_BARCODE",
                            "Barcode '" + barcode + "' is already used by another product in this tenant");
                });
    }

    /** Convert a product entity to its response DTO. */
    private ProductResponse toResponse(Product p, boolean stripCost) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getSku(),
                p.getBarcode(),
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getUnit(),
                p.getMrp(),
                p.getSellingPrice(),
                stripCost ? null : p.getCostPrice(),
                p.getGstRate(),
                p.getHsnCode(),
                p.getCurrentStock(),
                p.getReorderLevel(),
                p.stockStatus(),
                p.isActive(),
                p.getCreatedAt()
        );
    }

    /** True if the caller has the CASHIER role (cost data must be hidden). */
    private boolean isCashier() {
        UserPrincipal principal = currentPrincipal();
        return principal != null && principal.getRole() == UserRole.CASHIER;
    }

    private UserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Category management
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        UUID tenantId = TenantContext.require();
        return categoryRepository.findByTenantIdOrderBySortOrderAscNameAsc(tenantId)
                .stream().map(this::toCategoryResponse).collect(Collectors.toList());
    }

    public CategoryResponse createCategory(CreateCategoryRequest req) {
        UUID tenantId = TenantContext.require();
        UserPrincipal caller = currentPrincipal();

        categoryRepository.findByTenantIdAndName(tenantId, req.name())
                .ifPresent(existing -> {
                    throw new BusinessException("DUPLICATE_CATEGORY",
                            "Category '" + req.name() + "' already exists in this tenant");
                });

        Category category = new Category();
        category.setTenantId(tenantId);
        category.setName(req.name());
        category.setSortOrder(req.sortOrder());
        category.setActive(req.active() == null || req.active());
        if (caller != null) category.setCreatedBy(caller.getId());

        return toCategoryResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(UUID id, CreateCategoryRequest req) {
        UUID tenantId = TenantContext.require();
        UserPrincipal caller = currentPrincipal();

        Category category = categoryRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Category", id));

        categoryRepository.findByTenantIdAndName(tenantId, req.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("DUPLICATE_CATEGORY",
                            "Category '" + req.name() + "' already exists in this tenant");
                });

        category.setName(req.name());
        category.setSortOrder(req.sortOrder());
        if (req.active() != null) category.setActive(req.active());
        if (caller != null) category.setUpdatedBy(caller.getId());

        return toCategoryResponse(categoryRepository.save(category));
    }

    public void deleteCategory(UUID id) {
        UUID tenantId = TenantContext.require();

        Category category = categoryRepository.findById(id)
                .filter(c -> tenantId.equals(c.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Category", id));

        category.setActive(false);
        categoryRepository.save(category);
        log.info("Category soft-deleted: id={} tenant={}", id, tenantId);
    }

    private CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSortOrder(), c.isActive(), c.getCreatedAt());
    }

    // -------------------------------------------------------------------------
    // In-memory predicate helpers (upgrade to Specification for large catalogs)
    // -------------------------------------------------------------------------

    private boolean matchesSearch(Product p, String q) {
        if (q == null || q.isBlank()) return true;
        String lower = q.toLowerCase();
        return (p.getName() != null    && p.getName().toLowerCase().contains(lower))
            || (p.getSku() != null     && p.getSku().toLowerCase().contains(lower))
            || (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(lower));
    }

    private boolean matchesCategory(Product p, String category) {
        if (category == null || category.isBlank()) return true;
        return p.getCategory() != null
                && category.equalsIgnoreCase(p.getCategory().getName());
    }

    private boolean matchesUnit(Product p, String unit) {
        if (unit == null || unit.isBlank()) return true;
        try {
            return p.getUnit() == ProductUnit.valueOf(unit.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean matchesStatus(Product p, String status) {
        if (status == null || status.isBlank()) return true;
        return switch (status.toLowerCase()) {
            case "in"       -> p.stockStatus() == StockStatus.IN;
            case "low"      -> p.stockStatus() == StockStatus.LOW;
            case "out"      -> p.stockStatus() == StockStatus.OUT;
            case "active"   -> p.isActive();
            case "inactive" -> !p.isActive();
            default         -> true;
        };
    }
}
