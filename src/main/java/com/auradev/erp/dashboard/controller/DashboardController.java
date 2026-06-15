package com.auradev.erp.dashboard.controller;

import com.auradev.erp.dashboard.dto.DashboardLiveResponse;
import com.auradev.erp.dashboard.dto.DashboardMetricsResponse;
import com.auradev.erp.dashboard.dto.DashboardResponse;
import com.auradev.erp.dashboard.dto.DashboardShellResponse;
import com.auradev.erp.dashboard.dto.DashboardTrendResponse;
import com.auradev.erp.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Store overview KPIs, trends, and activity")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Store-wide dashboard shell", description = "Tenant, low stock, activity — load once, not tied to period filters")
    @GetMapping("/shell")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardShellResponse> shell() {
        return ResponseEntity.ok(dashboardService.getShell());
    }

    @Operation(summary = "Filter-dependent dashboard metrics", description = "KPIs, trend, top products, recent bills — reload when filters change")
    @GetMapping("/metrics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardMetricsResponse> metrics(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID productId) {
        return ResponseEntity.ok(dashboardService.getMetrics(
                effectivePreset(preset, range), from, to, customerId, productId));
    }

    @Operation(summary = "Full dashboard (initial load)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardResponse> get(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID productId) {
        return ResponseEntity.ok(dashboardService.getDashboard(
                effectivePreset(preset, range), from, to, customerId, productId));
    }

    @Operation(summary = "Live dashboard slice", description = "KPIs, recent bills, stock alerts — for 60s polling without reloading charts")
    @GetMapping("/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardLiveResponse> live(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID productId) {
        return ResponseEntity.ok(dashboardService.getDashboardLive(
                effectivePreset(preset, range), from, to, customerId, productId));
    }

    @Operation(summary = "Sales trend chart only", description = "Reload chart when period or filters change")
    @GetMapping("/trend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardTrendResponse> trend(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID productId) {
        return ResponseEntity.ok(dashboardService.getSalesTrend(
                effectivePreset(preset, range), from, to, customerId, productId));
    }

    private static String effectivePreset(String preset, String range) {
        if (preset != null && !preset.isBlank()) {
            return preset;
        }
        if (range != null && !range.isBlank()) {
            return range;
        }
        return "week";
    }
}
