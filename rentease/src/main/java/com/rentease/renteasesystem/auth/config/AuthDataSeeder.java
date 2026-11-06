package com.rentease.renteasesystem.auth.config;

import com.rentease.renteasesystem.auth.model.User;
import com.rentease.renteasesystem.auth.repository.UserRepository;
import com.rentease.renteasesystem.auth.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SHARED - Authentication & Access Control Module
 *
 * Seeds one admin and one demo customer on first start so the app is usable
 * straight away. Runs only when the table is empty, so it will not overwrite
 * real accounts. Change these credentials before any public deployment.
 */
@Configuration
public class AuthDataSeeder {

    @Bean
    CommandLineRunner seedAccounts(UserRepository userRepository, AuthService authService) {
        return args -> {
            if (userRepository.count() > 0) {
                return;
            }

            authService.createAccount("System Administrator", "admin@rentease.lk",
                    "Admin@123", User.Role.ADMIN);
            authService.createAccount("Demo Customer", "user@rentease.lk",
                    "User@123", User.Role.USER);

            System.out.println("""

                    ============================================================
                     RentEase - default accounts created
                       Admin portal  : /admin/login
                         admin@rentease.lk / Admin@123
                       Customer portal: /login
                         user@rentease.lk / User@123
                     Change these before deploying.
                    ============================================================
                    """);
        };
    }
}
