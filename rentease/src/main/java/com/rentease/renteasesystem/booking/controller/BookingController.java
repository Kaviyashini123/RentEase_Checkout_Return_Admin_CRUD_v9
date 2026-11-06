package com.rentease.renteasesystem.booking.controller;

import org.springframework.stereotype.Controller;
import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rentease.renteasesystem.booking.model.Favorite;
import com.rentease.renteasesystem.booking.model.Vehicle;
import com.rentease.renteasesystem.booking.service.VehicleSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MEMBER 1 - Pravein M. (IT25101778)
 * Vehicle Search & Browsing Module - Controller
 * MVC endpoints render Thymeleaf views; /api/** endpoints return JSON for AJAX filtering.
 */
@Controller
@RequestMapping("/booking")
public class BookingController {

    private final VehicleSearchService vehicleSearchService;

    public BookingController(VehicleSearchService vehicleSearchService) {
        this.vehicleSearchService = vehicleSearchService;
    }

    @org.springframework.web.bind.annotation.GetMapping
    public String searchPage(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String transmission,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Model model) {

        // Empty HTML select/input values must be converted to null.
        // Otherwise an empty value becomes a real filter and SQL returns no rows.
        Vehicle.VehicleType vehicleType = parseEnum(type, Vehicle.VehicleType.class);
        Vehicle.TransmissionType transmissionType = parseEnum(transmission, Vehicle.TransmissionType.class);
        Vehicle.FuelType vehicleFuelType = parseEnum(fuelType, Vehicle.FuelType.class);
        String cleanLocation = blankToNull(location);

        List<Vehicle> results = vehicleSearchService.searchVehicles(
                vehicleType, cleanLocation, transmissionType, vehicleFuelType, minPrice, maxPrice);

        model.addAttribute("vehicles", results);
        model.addAttribute("types", Vehicle.VehicleType.values());
        model.addAttribute("transmissions", Vehicle.TransmissionType.values());
        model.addAttribute("fuelTypes", Vehicle.FuelType.values());
        model.addAttribute("selectedType", vehicleType);
        model.addAttribute("selectedTransmission", transmissionType);
        model.addAttribute("selectedFuelType", vehicleFuelType);
        model.addAttribute("selectedLocation", cleanLocation);
        return "booking/search";
    }

    private static String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    private static <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass) {
        String cleaned = blankToNull(value);
        if (cleaned == null) return null;
        try {
            return Enum.valueOf(enumClass, cleaned.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ---- Booking UI CRUD: vehicle catalogue lives inside Booking ----
    @GetMapping("/manage")
    public String manageVehicles(Model model) {
        List<Vehicle> vehicles = vehicleSearchService.getAllVehicles();
        Map<String, Long> statusCounts = new HashMap<>();
        for (Vehicle.VehicleStatus status : Vehicle.VehicleStatus.values()) {
            statusCounts.put(status.name(), vehicles.stream().filter(vehicle -> vehicle.getStatus() == status).count());
        }
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("fleetTotal", vehicles.size());
        model.addAttribute("fleetStatusCounts", statusCounts);
        model.addAttribute("vehicleStatuses", Vehicle.VehicleStatus.values());
        model.addAttribute("vehicleTypes", Vehicle.VehicleType.values());
        return "booking/manage";
    }

    @GetMapping("/new")
    public String newVehicle(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        addVehicleOptions(model);
        return "booking/vehicle-form";
    }

    @PostMapping("/new")
    public String createVehicle(@Valid @ModelAttribute("vehicle") Vehicle vehicle,
                                org.springframework.validation.BindingResult bindingResult,
                                Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) { addVehicleOptions(model); return "booking/vehicle-form"; }
        try { vehicleSearchService.createVehicle(vehicle); redirectAttributes.addFlashAttribute("success", "Vehicle created successfully."); return "redirect:/booking/manage"; }
        catch (IllegalArgumentException ex) { model.addAttribute("error", ex.getMessage()); addVehicleOptions(model); return "booking/vehicle-form"; }
    }

    @GetMapping("/edit/{id}")
    public String editVehicle(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", vehicleSearchService.getVehicleDetails(id));
        addVehicleOptions(model);
        return "booking/vehicle-form";
    }

    @PostMapping("/edit/{id}")
    public String updateVehicle(@PathVariable Long id, @Valid @ModelAttribute("vehicle") Vehicle vehicle,
                                org.springframework.validation.BindingResult bindingResult,
                                Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) { vehicle.setId(id); addVehicleOptions(model); return "booking/vehicle-form"; }
        try { vehicleSearchService.updateVehicle(id, vehicle); redirectAttributes.addFlashAttribute("success", "Vehicle updated successfully."); return "redirect:/booking/manage"; }
        catch (IllegalArgumentException ex) { vehicle.setId(id); model.addAttribute("error", ex.getMessage()); addVehicleOptions(model); return "booking/vehicle-form"; }
    }

    @PostMapping("/delete/{id}")
    public String deleteVehicle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        vehicleSearchService.deleteVehicle(id); redirectAttributes.addFlashAttribute("success", "Vehicle deleted successfully.");
        return "redirect:/booking/manage";
    }

    private void addVehicleOptions(Model model) {
        model.addAttribute("types", Vehicle.VehicleType.values());
        model.addAttribute("transmissions", Vehicle.TransmissionType.values());
        model.addAttribute("fuelTypes", Vehicle.FuelType.values());
        model.addAttribute("statuses", Vehicle.VehicleStatus.values());
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public String vehicleDetails(@PathVariable Long id, Model model) {
        model.addAttribute("vehicle", vehicleSearchService.getVehicleDetails(id));
        return "booking/details";
    }

    // ---- JSON API endpoints (for AJAX / mobile) ----

    @RestController
    @RequestMapping("/api/booking")
    public static class VehicleSearchApiController {

        private final VehicleSearchService vehicleSearchService;

        public VehicleSearchApiController(VehicleSearchService vehicleSearchService) {
            this.vehicleSearchService = vehicleSearchService;
        }

        @GetMapping
        public ResponseEntity<List<Vehicle>> apiSearch(
                @RequestParam(required = false) Vehicle.VehicleType type,
                @RequestParam(required = false) String location,
                @RequestParam(required = false) Vehicle.TransmissionType transmission,
                @RequestParam(required = false) Vehicle.FuelType fuelType,
                @RequestParam(required = false) BigDecimal minPrice,
                @RequestParam(required = false) BigDecimal maxPrice) {
            return ResponseEntity.ok(vehicleSearchService.searchVehicles(type, location, transmission, fuelType, minPrice, maxPrice));
        }

        @GetMapping("/quick")
        public ResponseEntity<List<Vehicle>> quickSearch(@RequestParam String keyword) {
            return ResponseEntity.ok(vehicleSearchService.quickSearch(keyword));
        }

        @GetMapping("/{id}")
        public ResponseEntity<Vehicle> getVehicle(@PathVariable Long id) {
            return ResponseEntity.ok(vehicleSearchService.getVehicleDetails(id));
        }

        @PostMapping("/favorites")
        public ResponseEntity<Favorite> addFavorite(@RequestParam Long customerId, @RequestParam Long vehicleId) {
            return ResponseEntity.ok(vehicleSearchService.addFavorite(customerId, vehicleId));
        }

        @DeleteMapping("/favorites")
        public ResponseEntity<Void> removeFavorite(@RequestParam Long customerId, @RequestParam Long vehicleId) {
            vehicleSearchService.removeFavorite(customerId, vehicleId);
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/favorites/{customerId}")
        public ResponseEntity<List<Favorite>> getFavorites(@PathVariable Long customerId) {
            return ResponseEntity.ok(vehicleSearchService.getFavoritesForCustomer(customerId));
        }

        // Booking module CRUD: vehicle catalog is managed here together with search.
        @PostMapping("/vehicles")
        public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody Vehicle vehicle) {
            vehicle.setId(null);
            return ResponseEntity.status(201).body(vehicleSearchService.createVehicle(vehicle));
        }

        @GetMapping("/vehicles")
        public ResponseEntity<List<Vehicle>> getVehicles() {
            return ResponseEntity.ok(vehicleSearchService.getAllVehicles());
        }

        @GetMapping("/vehicles/{id}")
        public ResponseEntity<Vehicle> getVehicleCrud(@PathVariable Long id) {
            return ResponseEntity.ok(vehicleSearchService.getVehicleDetails(id));
        }

        @PutMapping("/vehicles/{id}")
        public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @Valid @RequestBody Vehicle vehicle) {
            return ResponseEntity.ok(vehicleSearchService.updateVehicle(id, vehicle));
        }

        @DeleteMapping("/vehicles/{id}")
        public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
            vehicleSearchService.deleteVehicle(id);
            return ResponseEntity.noContent().build();
        }
    }
}
