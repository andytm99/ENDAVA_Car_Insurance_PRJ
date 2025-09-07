package com.example.carins.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.carins.model.Car;
import com.example.carins.repo.InsuranceClaimRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.service.CarService;
import com.example.carins.web.dto.CarDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CarController {

    private final CarService service;
    private final InsuranceClaimRepository claimRepo;
    private final InsurancePolicyRepository policyRepo;

    public CarController(CarService service, InsuranceClaimRepository claimRepo, InsurancePolicyRepository policyRepo) {
        this.service = service;
        this.claimRepo = claimRepo;
        this.policyRepo = policyRepo;
    }

    /**
     * Register an insurance claim for a car Request: { "claimDate":
     * "2025-09-06", "description": "Accident", "amount": 1200.50 } Response:
     * 201 Created, Location header, body: created claim
     */
    @PostMapping("/cars/{carId}/claims")
    public ResponseEntity<?> registerClaim(@PathVariable Long carId,
            @Valid @RequestBody com.example.carins.web.dto.InsuranceClaimDto dto,
            org.springframework.validation.BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .findFirst().orElse("Validation error");
            return ResponseEntity.badRequest().body(msg);
        }
        var carOpt = service.listCars().stream().filter(c -> c.getId().equals(carId)).findFirst();
        if (carOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Car not found");
        }
        var claim = new com.example.carins.model.InsuranceClaim();
        claim.setCar(carOpt.get());
        claim.setClaimDate(dto.getClaimDate());
        claim.setDescription(dto.getDescription());
        claim.setAmount(dto.getAmount());
        claimRepo.save(claim);
        return ResponseEntity.created(java.net.URI.create("/api/cars/" + carId + "/claims/" + claim.getId())).body(claim);
    }

    /**
     * Get the history of a car (claims and policies) Response: [ { "type":
     * "CLAIM", "date": "2025-09-06", "description": "...", "amount": 1200.50 },
     * ... ] Returns 404 if carId does not exist
     */
    @GetMapping("/cars/{carId}/history")
    public ResponseEntity<?> getCarHistory(@PathVariable Long carId) {
        var carOpt = service.listCars().stream().filter(c -> c.getId().equals(carId)).findFirst();
        if (carOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Car not found");
        }
        var claims = claimRepo.findByCarIdOrderByClaimDateAsc(carId);
        var policies = policyRepo.findAll().stream().filter(p -> p.getCar().getId().equals(carId)).toList();
        java.util.List<java.util.Map<String, Object>> events = new java.util.ArrayList<>();
        for (var p : policies) {
            java.util.Map<String, Object> e = new java.util.HashMap<>();
            e.put("type", "POLICY");
            e.put("startDate", p.getStartDate());
            e.put("endDate", p.getEndDate());
            e.put("provider", p.getProvider());
            events.add(e);
        }
        for (var c : claims) {
            java.util.Map<String, Object> e = new java.util.HashMap<>();
            e.put("type", "CLAIM");
            e.put("date", c.getClaimDate());
            e.put("description", c.getDescription());
            e.put("amount", c.getAmount());
            events.add(e);
        }
        events.sort(java.util.Comparator.comparing(e -> e.containsKey("date") ? (java.time.LocalDate) e.get("date") : (java.time.LocalDate) e.get("startDate")));
        return ResponseEntity.ok(events);
    }

    @GetMapping("/cars")
    public List<CarDto> getCars() {
        return service.listCars().stream().map(this::toDto).toList();
    }

    @GetMapping("/cars/{carId}/insurance-valid")
    public ResponseEntity<?> isInsuranceValid(@PathVariable Long carId, @RequestParam String date) {
        // Validate car existence
        var carOpt = service.listCars().stream().filter(c -> c.getId().equals(carId)).findFirst();
        if (carOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Car not found");
        }

        // Validate date format
        LocalDate d;
        try {
            d = LocalDate.parse(date);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Invalid date format. Use ISO YYYY-MM-DD.");
        }

        // Reject impossible dates
        if (d.isBefore(LocalDate.of(1900, 1, 1)) || d.isAfter(LocalDate.of(2100, 12, 31))) {
            return ResponseEntity.badRequest().body("Date out of supported range (1900-01-01 to 2100-12-31).");
        }

        boolean valid = service.isInsuranceValid(carId, d);
        return ResponseEntity.ok(new InsuranceValidityResponse(carId, d.toString(), valid));
    }

    private CarDto toDto(Car c) {
        var o = c.getOwner();
        return new CarDto(c.getId(), c.getVin(), c.getMake(), c.getModel(), c.getYearOfManufacture(),
                o != null ? o.getId() : null,
                o != null ? o.getName() : null,
                o != null ? o.getEmail() : null);
    }

    public record InsuranceValidityResponse(Long carId, String date, boolean valid) {

    }
}
