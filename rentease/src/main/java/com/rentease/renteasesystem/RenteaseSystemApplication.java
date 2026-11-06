package com.rentease.renteasesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RentEase - Main Application Entry Point
 * Shared bootstrap class - all 5 team modules are auto-discovered
 * via component scanning since they live under the com.rentease.renteasesystem
 * base package (booking, reservation, payment, checkout, reports).
 *
 */
@SpringBootApplication
public class RenteaseSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(RenteaseSystemApplication.class, args);
    }
}
