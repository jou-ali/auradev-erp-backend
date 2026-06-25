package com.auradev.erp.common.web;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestIpHelper {

    private RequestIpHelper() {}

    public static String clientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
