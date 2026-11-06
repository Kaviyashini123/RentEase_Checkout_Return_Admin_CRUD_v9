package com.rentease.renteasesystem.auth.model;

import java.io.Serializable;

/**
 * SHARED - Authentication & Access Control Module
 *
 * What we put in the HTTP session after a successful login. Deliberately
 * excludes the password hash, so the credential never sits in session memory
 * or gets exposed to a Thymeleaf template by accident.
 */
public class SessionUser implements Serializable {

    public static final String SESSION_KEY = "currentUser";

    private final Long id;
    private final String fullName;
    private final String email;
    private final User.Role role;

    public SessionUser(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public User.Role getRole() { return role; }

    public boolean isAdmin() { return role == User.Role.ADMIN; }

    /** First name only - used for the navbar greeting. */
    public String getDisplayName() {
        if (fullName == null || fullName.isBlank()) return email;
        return fullName.trim().split("\\s+")[0];
    }
}
