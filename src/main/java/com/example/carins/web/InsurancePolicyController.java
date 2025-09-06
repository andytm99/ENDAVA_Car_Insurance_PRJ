package com.example.carins.web;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.carins.model.Car;
import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.web.dto.InsurancePolicyDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/policies")
public class InsurancePolicyController {

    private final InsurancePolicyRepository policyRepo;
    private final CarRepository carRepo;

    public InsurancePolicyController(InsurancePolicyRepository policyRepo, CarRepository carRepo) {
        this.policyRepo = policyRepo;
        this.carRepo = carRepo;
    }

    @PostMapping
    public ResponseEntity<?> createPolicy(@Valid @RequestBody InsurancePolicyDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .findFirst()
                    .orElse("Validation error");
            return ResponseEntity.badRequest().body(msg);
        }
        Car car = carRepo.findById(dto.getCarId()).orElse(null);
        if (car == null) {
            return ResponseEntity.badRequest().body("Car not found");
        }
        InsurancePolicy policy = new InsurancePolicy(car, dto.getProvider(), dto.getStartDate(), dto.getEndDate());
        policyRepo.save(policy);
        return ResponseEntity.ok(policy);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePolicy(@PathVariable Long id, @Valid @RequestBody InsurancePolicyDto dto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().stream()
                    .map(e -> e.getDefaultMessage())
                    .findFirst()
                    .orElse("Validation error");
            return ResponseEntity.badRequest().body(msg);
        }
        InsurancePolicy policy = policyRepo.findById(id).orElse(null);
        if (policy == null) {
            return ResponseEntity.notFound().build();
        }
        Car car = carRepo.findById(dto.getCarId()).orElse(null);
        if (car == null) {
            return ResponseEntity.badRequest().body("Car not found");
        }
        policy.setCar(car);
        policy.setProvider(dto.getProvider());
        policy.setStartDate(dto.getStartDate());
        policy.setEndDate(dto.getEndDate());
        policyRepo.save(policy);
        return ResponseEntity.ok(policy);
    }
}
