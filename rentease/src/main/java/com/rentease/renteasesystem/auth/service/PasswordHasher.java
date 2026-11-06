package com.rentease.renteasesystem.auth.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SHARED - Authentication & Access Control Module
 *
 * Salted SHA-256 hashing. Uses only the JDK so the project needs no new
 * Maven dependency. For a production system, swap this for BCrypt:
 * add spring-security-crypto and delegate to BCryptPasswordEncoder.
 */
@Component
public class PasswordHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Returns "salt:hash", both Base64 encoded. */
    public String hash(String rawPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltPart = Base64.getEncoder().encodeToString(salt);
        return saltPart + ":" + digest(rawPassword, saltPart);
    }

    /** Constant-time comparison of a raw password against a stored "salt:hash". */
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        String[] parts = storedHash.split(":", 2);
        String expected = parts[1];
        String actual = digest(rawPassword, parts[0]);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String digest(String rawPassword, String saltPart) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(saltPart));
            byte[] hashed = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
