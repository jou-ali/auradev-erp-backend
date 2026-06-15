package com.auradev.erp.dashboard.service;

import com.auradev.erp.dashboard.dto.DashboardScope;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

final class DashboardScopeResolver {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final int MAX_TREND_POINTS = 31;
    private static final int MAX_CUSTOM_DAYS = 366;

    private DashboardScopeResolver() {}

    static DashboardScope resolve(
            String preset,
            LocalDate from,
            LocalDate to,
            UUID customerId,
            UUID productId) {

        String effective = normalizePreset(preset);
        LocalDate today = LocalDate.now(STORE_ZONE);

        LocalDate periodStart;
        LocalDate periodEnd;

        switch (effective) {
            case "today" -> {
                periodStart = today;
                periodEnd = today;
            }
            case "yesterday" -> {
                periodStart = today.minusDays(1);
                periodEnd = today.minusDays(1);
            }
            case "month" -> {
                periodEnd = today;
                periodStart = today.minusDays(29);
            }
            case "custom" -> {
                if (from == null || to == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Custom range requires both from and to dates (YYYY-MM-DD)");
                }
                periodStart = from;
                periodEnd = to;
                if (periodEnd.isBefore(periodStart)) {
                    LocalDate swap = periodStart;
                    periodStart = periodEnd;
                    periodEnd = swap;
                }
                if (periodEnd.isAfter(today)) {
                    periodEnd = today;
                }
                if (periodStart.isAfter(today)) {
                    periodStart = today;
                }
                if (periodEnd.isBefore(periodStart)) {
                    periodStart = periodEnd;
                }
                long span = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
                if (span > MAX_CUSTOM_DAYS) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Custom range cannot exceed " + MAX_CUSTOM_DAYS + " days");
                }
            }
            default -> {
                effective = "week";
                periodEnd = today;
                periodStart = today.minusDays(6);
            }
        }

        long periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        LocalDate compareEnd = periodStart.minusDays(1);
        LocalDate compareStart = compareEnd.minusDays(periodDays - 1);

        Instant periodFrom = periodStart.atStartOfDay(STORE_ZONE).toInstant();
        Instant periodTo = periodEnd.plusDays(1).atStartOfDay(STORE_ZONE).toInstant();
        Instant compareFrom = compareStart.atStartOfDay(STORE_ZONE).toInstant();
        Instant compareTo = periodFrom;

        String periodLabel = formatPeriodLabel(periodStart, periodEnd, effective);
        String compareLabel = periodDays == 1
                ? "vs previous day"
                : "vs prior " + periodDays + " days";

        int trendPointCount = (int) Math.min(periodDays, MAX_TREND_POINTS);

        return new DashboardScope(
                effective,
                periodFrom,
                periodTo,
                compareFrom,
                compareTo,
                customerId,
                productId,
                periodLabel,
                compareLabel,
                trendPointCount);
    }

    private static String normalizePreset(String preset) {
        if (preset == null || preset.isBlank()) {
            return "week";
        }
        return preset.trim().toLowerCase(Locale.ROOT);
    }

    private static String formatPeriodLabel(LocalDate start, LocalDate end, String preset) {
        return switch (preset) {
            case "today" -> "Today · " + start.format(DAY_FMT);
            case "yesterday" -> "Yesterday · " + start.format(DAY_FMT);
            case "week" -> "Last 7 days";
            case "month" -> "Last 30 days";
            case "custom" -> start.equals(end)
                    ? start.format(DAY_FMT)
                    : start.format(DAY_FMT) + " – " + end.format(DAY_FMT);
            default -> start.format(DAY_FMT) + " – " + end.format(DAY_FMT);
        };
    }
}
