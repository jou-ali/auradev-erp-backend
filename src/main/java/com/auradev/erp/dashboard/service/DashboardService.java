package com.auradev.erp.dashboard.service;

import com.auradev.erp.auth.security.UserPrincipal;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Kolkata");

    private final DashboardQueryRepository queries;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public DashboardResponse getDashboard(String range) {
        UUID tenantId = TenantContext.require();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        LocalDate today = LocalDate.now(STORE_ZONE);
        Instant todayFrom = today.atStartOfDay(STORE_ZONE).toInstant();
        Instant todayTo = today.plusDays(1).atStartOfDay(STORE_ZONE).toInstant();
        Instant yesterdayFrom = today.minusDays(1).atStartOfDay(STORE_ZONE).toInstant();
        Instant yesterdayTo = todayFrom;

        DashboardKpis kpis = new DashboardKpis(
                queries.sumSalesBetween(tenantId, todayFrom, todayTo),
                queries.sumSalesBetween(tenantId, yesterdayFrom, yesterdayTo),
                queries.countBillsBetween(tenantId, todayFrom, todayTo),
                queries.countBillsBetween(tenantId, yesterdayFrom, yesterdayTo),
                queries.sumItemsSoldBetween(tenantId, todayFrom, todayTo),
                queries.sumItemsSoldBetween(tenantId, yesterdayFrom, yesterdayTo),
                queries.countLowStock(tenantId)
        );

        int trendDays = switch (range != null ? range : "week") {
            case "today" -> 1;
            case "month" -> 30;
            default -> 7;
        };

        return new DashboardResponse(
                tenant.getName(),
                currentUserName(),
                kpis,
                queries.salesTrend(tenantId, trendDays),
                queries.topProductsToday(tenantId, 5),
                queries.recentBills(tenantId, 10),
                queries.lowStockProducts(tenantId, 6),
                queries.recentActivity(tenantId, 8),
                buildAiBrief(kpis)
        );
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

    private String buildAiBrief(DashboardKpis kpis) {
        String salesTrend = formatTrend(kpis.todaySales(), kpis.yesterdaySales());
        int low = kpis.lowStockCount();
        if (low > 0) {
            return "Sales are pacing " + salesTrend + " vs yesterday. "
                    + low + " product" + (low == 1 ? " is" : "s are") + " at or below reorder level.";
        }
        return "Sales are pacing " + salesTrend + " vs yesterday. Stock levels look healthy.";
    }

    private String formatTrend(BigDecimal today, BigDecimal yesterday) {
        if (yesterday == null || yesterday.compareTo(BigDecimal.ZERO) == 0) {
            if (today != null && today.compareTo(BigDecimal.ZERO) > 0) {
                return "up from zero";
            }
            return "steady";
        }
        BigDecimal pct = today.subtract(yesterday)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterday, 1, RoundingMode.HALF_UP);
        if (pct.compareTo(BigDecimal.ZERO) > 0) {
            return "+" + pct.stripTrailingZeros().toPlainString() + "%";
        }
        if (pct.compareTo(BigDecimal.ZERO) < 0) {
            return pct.stripTrailingZeros().toPlainString() + "%";
        }
        return "steady";
    }
}
