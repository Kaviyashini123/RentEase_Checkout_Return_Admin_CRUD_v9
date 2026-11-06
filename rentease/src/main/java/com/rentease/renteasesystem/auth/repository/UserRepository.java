package com.rentease.renteasesystem.auth.repository;

import com.rentease.renteasesystem.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * SHARED - Authentication & Access Control Module
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(User.Role role);
}
