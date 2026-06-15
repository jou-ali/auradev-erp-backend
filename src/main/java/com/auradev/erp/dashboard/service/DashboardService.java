package com.auradev.erp.dashboard.service;

import com.auradev.erp.auth.security.UserPrincipal;
import com.auradev.erp.billing.repository.CustomerRepository;
import com.auradev.erp.catalog.repository.ProductRepository;
import com.auradev.erp.dashboard.dto.*;
import com.auradev.erp.dashboard.repository.DashboardQueryRepository;
import com.auradev.erp.tenant.TenantContext;
import com.auradev.erp.tenant.entity.Tenant;
import com.auradev.erp.tenant.repository.TenantRepository;
import com.auradev.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardQueryRepository queries;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public DashboardShellResponse getShell() {
        UUID tenantId = TenantContext.require();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));
        return new DashboardShellResponse(
                tenant.getName(),
                currentUserName(),
                queries.countLowStock(tenantId),
                queries.lowStockProducts(tenantId, 6),
                queries.recentActivity(tenantId, 8));
    }

    public DashboardMetricsResponse getMetrics(
            String preset,
            LocalDate from,
            LocalDate to,
            UUID customerId,
            UUID productId) {

        UUID tenantId = TenantContext.require();
        DashboardScope scope = DashboardScopeResolver.resolve(preset, from, to, customerId, productId);
        DashboardMeta meta = buildMeta(scope);
        DashboardKpis kpis = queries.fetchKpis(tenantId, scope);

        return new DashboardMetricsResponse(
                meta,
                kpis,
                queries.salesTrend(tenantId, scope),
                queries.topProductsInScope(tenantId, scope, 5),
                queries.recentBillsInScope(tenantId, scope, 10),
                buildAiBrief(kpis, meta));
    }

    public DashboardResponse getDashboard(
            String preset,
            LocalDate from,
            LocalDate to,
            UUID customerId,
            UUID productId) {

        UUID tenantId = TenantContext.require();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));
        DashboardScope scope = DashboardScopeResolver.resolve(preset, from, to, customerId, productId);
        DashboardMeta meta = buildMeta(scope);

        DashboardKpis kpis = queries.fetchKpis(tenantId, scope);

        return new DashboardResponse(
                tenant.getName(),
                currentUserName(),
                meta,
                kpis,
                queries.salesTrend(tenantId, scope),
                queries.topProductsInScope(tenantId, scope, 5),
                queries.recentBillsInScope(tenantId, scope, 10),
                queries.lowStockProducts(tenantId, 6),
                queries.recentActivity(tenantId, 8),
                buildAiBrief(kpis, meta));
    }

    public DashboardLiveResponse getDashboardLive(
            String preset,
            LocalDate from,
            LocalDate to,
            UUID customerId,
            UUID productId) {

        UUID tenantId = TenantContext.require();
        DashboardScope scope = DashboardScopeResolver.resolve(preset, from, to, customerId, productId);
        DashboardMeta meta = buildMeta(scope);
        DashboardKpis kpis = queries.fetchKpis(tenantId, scope);

        return new DashboardLiveResponse(
                null,
                null,
                meta,
                kpis,
                List.of(),
                queries.recentBillsInScope(tenantId, scope, 5),
                List.of(),
                List.of(),
                buildAiBrief(kpis, meta));
    }

    public DashboardTrendResponse getSalesTrend(
            String preset,
            LocalDate from,
            LocalDate to,
            UUID customerId,
            UUID productId) {

        UUID tenantId = TenantContext.require();
        DashboardScope scope = DashboardScopeResolver.resolve(preset, from, to, customerId, productId);
        return new DashboardTrendResponse(queries.salesTrend(tenantId, scope));
    }

    private DashboardMeta buildMeta(DashboardScope scope) {
        String customerName = scope.customerId() != null
                ? customerRepository.findById(scope.customerId()).map(c -> c.getName()).orElse("Customer")
                : null;
        String productName = scope.productId() != null
                ? productRepository.findById(scope.productId()).map(p -> p.getName()).orElse("Product")
                : null;
        boolean filtersActive = scope.customerId() != null
                || scope.productId() != null
                || "custom".equals(scope.preset())
                || "yesterday".equals(scope.preset());

        return new DashboardMeta(
                scope.preset(),
                scope.periodLabel(),
                scope.compareLabel(),
                customerName,
                productName,
                filtersActive);
    }

    private String currentUserName() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return userRepository.findById(principal.getId())
                    .map(u -> u.getName())
                    .orElse(principal.getEmail());
        }
        return "there";
    }

    private String buildAiBrief(DashboardKpis kpis, DashboardMeta meta) {
        String salesTrend = formatTrend(kpis.periodSales(), kpis.compareSales());
        int low = kpis.lowStockCount();
        String period = meta.periodLabel();
        String compare = meta.compareLabel();

        StringBuilder brief = new StringBuilder();
        brief.append(period).append(" sales are pacing ").append(salesTrend).append(" ").append(compare).append(".");

        if (meta.customerName() != null) {
            brief.append(" Filtered to ").append(meta.customerName()).append(".");
        }
        if (meta.productName() != null) {
            brief.append(" Showing ").append(meta.productName()).append(" only.");
        }

        if (low > 0) {
            brief.append(" ")
                    .append(low).append(" product").append(low == 1 ? " is" : "s are")
                    .append(" at or below reorder level.");
        } else if (!meta.filtersActive()) {
            brief.append(" Stock levels look healthy.");
        }

        return brief.toString();
    }

    private String formatTrend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current != null && current.compareTo(BigDecimal.ZERO) > 0) {
                return "up from zero";
            }
            return "steady";
        }
        BigDecimal pct = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
        if (pct.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + pct.stripTrailingZeros().toPlainString() + "%";
        }
        if (pct.compareTo(BigDecimal.ZERO) < 0) {
            return pct.stripTrailingZeros().toPlainString() + "%";
        }
        return "steady";
    }
}
