package com.rentease.renteasesystem.auth.controller;

import com.rentease.renteasesystem.auth.model.SessionUser;
import com.rentease.renteasesystem.auth.model.User;
import com.rentease.renteasesystem.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * SHARED - Authentication & Access Control Module - Controller
 *
 * Two independent login portals:
 *   /login        -> customers  (role USER)  -> lands on /booking
 *   /admin/login  -> staff      (role ADMIN) -> lands on /reports/dashboard
 */
@Controller
public class AuthController {

    private static final String USER_HOME = "redirect:/booking";
    private static final String ADMIN_HOME = "redirect:/reports/dashboard";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Root now decides where to send you based on who you are. */
    @GetMapping("/")
    public String home(HttpSession session) {
        SessionUser current = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (current == null) return "redirect:/login";
        return current.isAdmin() ? ADMIN_HOME : USER_HOME;
    }

    // ------------------------------------------------------------------
    // CUSTOMER PORTAL
    // ------------------------------------------------------------------

    @GetMapping("/login")
    public String userLoginPage(HttpSession session, Model model,
                                @RequestParam(required = false) String loggedOut) {
        SessionUser current = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (current != null) {
            return current.isAdmin() ? ADMIN_HOME : USER_HOME;
        }
        if (loggedOut != null) {
            model.addAttribute("info", "You have been signed out.");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String userLogin(@RequestParam String email,
                            @RequestParam String password,
                            HttpServletRequest request,
                            Model model) {
        try {
            User user = authService.authenticate(email, password, User.Role.USER);
            startSession(request, user);
            return USER_HOME;
        } catch (AuthService.AuthenticationException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email); // keep what they typed
            return "auth/login";
        }
    }

    // ------------------------------------------------------------------
    // ADMIN PORTAL
    // ------------------------------------------------------------------

    @GetMapping("/admin/login")
    public String adminLoginPage(HttpSession session, Model model,
                                 @RequestParam(required = false) String loggedOut) {
        SessionUser current = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (current != null && current.isAdmin()) {
            return ADMIN_HOME;
        }
        if (loggedOut != null) {
            model.addAttribute("info", "You have been signed out.");
        }
        return "auth/admin-login";
    }

    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String email,
                             @RequestParam String password,
                             HttpServletRequest request,
                             Model model) {
        try {
            User user = authService.authenticate(email, password, User.Role.ADMIN);
            startSession(request, user);
            return ADMIN_HOME;
        } catch (AuthService.AuthenticationException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            return "auth/admin-login";
        }
    }

    // ------------------------------------------------------------------
    // REGISTRATION (customers only)
    // ------------------------------------------------------------------

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam(required = false) String phone,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           HttpServletRequest request,
                           Model model) {
        try {
            User user = authService.register(fullName, email, phone, password, confirmPassword);
            startSession(request, user); // sign them straight in
            return USER_HOME;
        } catch (AuthService.AuthenticationException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            return "auth/register";
        }
    }

    // ------------------------------------------------------------------
    // LOGOUT / DENIED
    // ------------------------------------------------------------------

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        SessionUser current = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        boolean wasAdmin = current != null && current.isAdmin();
        session.invalidate();
        return wasAdmin ? "redirect:/admin/login?loggedOut" : "redirect:/login?loggedOut";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "auth/access-denied";
    }

    /**
     * Creates a fresh session on login so an attacker cannot fixate the
     * pre-login session id, then stores the sanitised user snapshot.
     */
    private void startSession(HttpServletRequest request, User user) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionUser.SESSION_KEY, new SessionUser(user));
        session.setMaxInactiveInterval(30 * 60); // 30 minutes
    }
}
