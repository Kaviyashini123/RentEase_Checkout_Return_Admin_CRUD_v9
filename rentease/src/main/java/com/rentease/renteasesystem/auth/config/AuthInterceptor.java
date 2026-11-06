package com.rentease.renteasesystem.auth.config;

import com.rentease.renteasesystem.auth.model.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * SHARED - Authentication & Access Control Module
 *
 * Server-side gate. Hiding a navbar link is not security - every protected
 * Protected module URLs are checked server-side and are not only hidden in the navbar.
 * Unauthenticated visitors are redirected to the appropriate login portal and
 * customers cannot enter staff-only areas.
 */
public class AuthInterceptor implements HandlerInterceptor {

    /** Open to everyone - the two login portals, registration, static assets. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/login", "/admin/login", "/register", "/logout", "/access-denied",
            "/css/", "/js/", "/images/", "/favicon.ico",
            "/h2-console", "/error"
    );

    /** Staff-only areas. Everything else just needs any signed-in user. */
    private static final List<String> ADMIN_PATHS = List.of(
            // Rental operations handled by staff/admin
            "/checkout",
            "/api/checkout",

            // Vehicle catalogue administration
            "/booking/manage",
            "/booking/new",
            "/booking/edit",
            "/booking/delete",
            "/api/booking/vehicles",

            // Payment / invoicing administration
            "/payment/manage",
            "/payment/new",
            "/payment/edit",
            "/payment/delete",
            "/api/payment/payments",
            "/api/payment/discount-codes",
            "/api/payment/invoices",

            // Reservation administration (customer booking remains separate)
            "/reservation/edit",

            // Reports and review moderation
            "/reports/dashboard",
            "/reports/studio",
            "/reports/export",
            "/reports/reviews",
            "/reports/review/management",
            "/reports/review/edit",
            "/reports/review/delete"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (isPublic(path)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        SessionUser current = (session == null)
                ? null
                : (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);

        boolean adminArea = path.equals("/payment") || isAdminArea(path);

        // Not signed in -> send to the portal that matches the area they wanted.
        if (current == null) {
            response.sendRedirect(request.getContextPath() + (adminArea ? "/admin/login" : "/login"));
            return false;
        }

        // Signed in as a customer but reaching for a staff screen.
        if (adminArea && !current.isAdmin()) {
            response.sendRedirect(request.getContextPath() + "/access-denied");
            return false;
        }

        return true;
    }

    private boolean isPublic(String path) {
        if (path.equals("/")) return true;
        return PUBLIC_PATHS.stream().anyMatch(p -> path.equals(p) || path.startsWith(p));
    }

    private boolean isAdminArea(String path) {
        return ADMIN_PATHS.stream().anyMatch(path::startsWith);
    }
}
