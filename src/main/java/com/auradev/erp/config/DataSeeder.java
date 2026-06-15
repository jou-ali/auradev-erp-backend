package com.auradev.erp.config;

import com.auradev.erp.billing.entity.Customer;
import com.auradev.erp.billing.entity.CustomerType;
import com.auradev.erp.billing.repository.CustomerRepository;
import com.auradev.erp.catalog.entity.Category;
import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.entity.UnitType;
import com.auradev.erp.catalog.repository.CategoryRepository;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.inventory.entity.Inventory;
import com.auradev.erp.inventory.repository.InventoryRepository;
import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import com.auradev.erp.user.entity.User;
import com.auradev.erp.user.entity.UserRole;
import com.auradev.erp.user.entity.UserStatus;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String[][] SEED_CATEGORIES = {
            {"Grains", "grains"},
            {"Dairy", "dairy"},
            {"Beverages", "beverages"},
            {"Personal Care", "personal-care"},
            {"Snacks", "snacks"},
    };

    private record SeedProduct(String name, String sku, String barcode, String categorySlug,
                               UnitType unitType, String unitLabel, String mrp, String price,
                               String cost, String tax, String stock, String reorder) {}

    private static final SeedProduct[] SEED_PRODUCTS = {
            new SeedProduct("Sona Masoori Rice", "GRN-RICE-25", "8901234500011", "grains",
                    UnitType.weight_kg, "kg", "78", "68", "58", "5", "320", "80"),
            new SeedProduct("Toor Dal (Arhar)", "GRN-TOOR-01", "8901234500028", "grains",
                    UnitType.weight_kg, "kg", "145", "132", "118", "5", "64", "70"),
            new SeedProduct("Nandini Toned Milk 500ml", "DRY-MILK-05", "8901234500073", "dairy",
                    UnitType.unit, "pcs", "26", "25", "23", "0", "142", "60"),
            new SeedProduct("Coca-Cola 750ml", "BEV-COK-75", "8901234500141", "beverages",
                    UnitType.unit, "pcs", "45", "42", "35", "18", "128", "60"),
            new SeedProduct("Parle-G Biscuits", "SNK-PRLG-01", "8901234500219", "snacks",
                    UnitType.unit, "pcs", "10", "10", "8", "18", "412", "150"),
            new SeedProduct("Lifebuoy Soap 125g", "PC-SOAP-12", "8901234500172", "personal-care",
                    UnitType.unit, "pcs", "38", "34", "28", "18", "186", "80"),
    };

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Tenant tenant = tenantRepository.count() > 0
                ? tenantRepository.findAll().get(0)
                : createTenant();

        seedCategories();
        seedProducts(tenant);
        seedCustomers(tenant);

        if (userRepository.findByEmail("admin@nenjankod.in").isEmpty()) {
            User admin = new User();
            admin.setTenantId(tenant.getId());
            admin.setName("Admin");
            admin.setEmail("admin@nenjankod.in");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setRole(UserRole.TENANT_ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            log.info("Seeded admin: admin@nenjankod.in / Admin@123");
        }
    }

    private Tenant createTenant() {
        Tenant tenant = new Tenant();
        tenant.setName("Nenjankod Supermarket");
        tenant.setPhone("0820 256 7711");
        tenant.setStateCode("29");
        tenant.setBillNoPrefix("NJK");
        tenant.setActive(true);
        return tenantRepository.save(tenant);
    }

    private void seedCategories() {
        int added = 0;
        for (String[] row : SEED_CATEGORIES) {
            if (categoryRepository.findBySlug(row[1]).isPresent()) continue;
            Category cat = new Category();
            cat.setName(row[0]);
            cat.setSlug(row[1]);
            cat.setActive(true);
            categoryRepository.save(cat);
            added++;
        }
        if (added > 0) {
            log.info("Seeded {} categories ({} total defined)", added, SEED_CATEGORIES.length);
        }
    }

    private void seedProducts(Tenant tenant) {
        if (productRepository.count() > 0) return;

        Map<String, Category> bySlug = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getSlug, Function.identity()));

        for (SeedProduct sp : SEED_PRODUCTS) {
            Category category = bySlug.get(sp.categorySlug());
            if (category == null) continue;

            Product product = new Product();
            product.setName(sp.name());
            product.setSku(sp.sku());
            product.setBarcode(sp.barcode());
            product.setCategory(category);
            product.setUnitType(sp.unitType());
            product.setUnitLabel(sp.unitLabel());
            product.setPriceMrp(new BigDecimal(sp.mrp()));
            product.setPriceSelling(new BigDecimal(sp.price()));
            product.setCostPrice(new BigDecimal(sp.cost()));
            product.setTaxRatePct(new BigDecimal(sp.tax()));
            product.setActive(true);
            product = productRepository.save(product);

            Inventory inv = new Inventory();
            inv.setTenantId(tenant.getId());
            inv.setProduct(product);
            inv.setQuantityOnHand(new BigDecimal(sp.stock()));
            inv.setLowStockThreshold(new BigDecimal(sp.reorder()));
            inv.setReorderQuantity(new BigDecimal(sp.reorder()));
            inv.setLastUpdated(Instant.now());
            inventoryRepository.save(inv);
        }
        log.info("Seeded {} sample products with inventory", SEED_PRODUCTS.length);
    }

    private void seedCustomers(Tenant tenant) {
        if (!customerRepository.findByTenantIdOrderByNameAsc(tenant.getId()).isEmpty()) return;

        Object[][] rows = {
                {"Walk-in Customer", null, CustomerType.walkin, 0, "0"},
                {"Ramesh Gowda", "9845211003", CustomerType.b2c, 480, "0"},
                {"Lakshmi Stores", "9901677254", CustomerType.b2b, 0, "4250"},
                {"Anitha Reddy", "9008045517", CustomerType.b2c, 1260, "0"},
                {"Suresh Kumar", "9448832109", CustomerType.b2c, 95, "320"},
                {"Sri Annapurna Mess", "9731560042", CustomerType.b2b, 0, "8900"},
        };

        for (Object[] row : rows) {
            Customer c = new Customer();
            c.setTenantId(tenant.getId());
            c.setName((String) row[0]);
            c.setPhone((String) row[1]);
            c.setType((CustomerType) row[2]);
            c.setLoyaltyPoints((Integer) row[3]);
            c.setCreditBalance(new BigDecimal((String) row[4]));
            customerRepository.save(c);
        }
        log.info("Seeded {} customers", rows.length);
    }
}
