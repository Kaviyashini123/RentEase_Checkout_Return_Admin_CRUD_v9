package com.rentease.renteasesystem.auth.service;

import com.rentease.renteasesystem.auth.model.User;
import com.rentease.renteasesystem.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * SHARED - Authentication & Access Control Module - Service
 *
 * The important rule here: authenticate(...) takes the role the login page
 * expects. A customer account cannot sign in through the admin page and an
 * admin account cannot sign in through the customer page, even if the
 * password is correct. The two login pages are therefore genuinely separate
 * entry points, not just two skins over one check.
 */
@Service
public class AuthService {

    /** Thrown for any failed login. Message is safe to show to the user. */
    public static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) { super(message); }
    }

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Verifies the credentials AND that the account carries the expected role.
     *
     * @param expectedRole the role the login portal is for (USER or ADMIN)
     * @throws AuthenticationException if credentials are wrong, the account is
     *         disabled, or the role does not match the portal
     */
    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword, User.Role expectedRole) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new AuthenticationException("Please enter both your email and password.");
        }

        Optional<User> found = userRepository.findByEmailIgnoreCase(email.trim());

        // Same generic message whether the email is unknown or the password is
        // wrong - avoids leaking which accounts exist.
        User user = found.orElseThrow(
                () -> new AuthenticationException("Invalid email or password."));

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated. Contact support.");
        }

        if (user.getRole() != expectedRole) {
            throw new AuthenticationException(expectedRole == User.Role.ADMIN
                    ? "This account is not an administrator account."
                    : "Administrator accounts must sign in through the admin portal.");
        }

        return user;
    }

    /** Self-registration - always creates a customer (USER) account. */
    @Transactional
    public User register(String fullName, String email, String phone,
                         String rawPassword, String confirmPassword) {

        if (fullName == null || fullName.isBlank()) {
            throw new AuthenticationException("Full name is required.");
        }
        if (email == null || !email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$")) {
            throw new AuthenticationException("Please enter a valid email address.");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new AuthenticationException("Password must be at least 6 characters.");
        }
        if (!rawPassword.equals(confirmPassword)) {
            throw new AuthenticationException("Passwords do not match.");
        }
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new AuthenticationException("An account with this email already exists.");
        }

        User user = new User(fullName.trim(), email.trim().toLowerCase(),
                passwordHasher.hash(rawPassword), User.Role.USER);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    /** Used by the seeder and by any future admin-management screen. */
    @Transactional
    public User createAccount(String fullName, String email, String rawPassword, User.Role role) {
        User user = new User(fullName, email.toLowerCase(), passwordHasher.hash(rawPassword), role);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }
}
