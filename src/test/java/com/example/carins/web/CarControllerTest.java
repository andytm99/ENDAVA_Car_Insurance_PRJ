package com.example.carins.web;

import com.example.carins.model.Car;
import com.example.carins.model.InsuranceClaim;
import com.example.carins.service.CarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarController.class)
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarService carService;

    @MockBean
    private com.example.carins.repo.InsurancePolicyRepository insurancePolicyRepository;

    @MockBean
    private com.example.carins.repo.InsuranceClaimRepository insuranceClaimRepository;

    // Mock a car for valid tests
    private Car mockCar() {
        Car car = new Car();
        try {
            var field = Car.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(car, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        car.setVin("VIN123");
        return car;
    }

    @Test
    void insuranceValid_returns404ForMissingCar() throws Exception {
        when(carService.listCars()).thenReturn(List.of()); // No cars
        mockMvc.perform(get("/api/cars/999/insurance-valid?date=2025-09-07"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Car not found"));
    }

    @Test
    void insuranceValid_returns400ForInvalidDateFormat() throws Exception {
        when(carService.listCars()).thenReturn(List.of(mockCar()));
        mockMvc.perform(get("/api/cars/1/insurance-valid?date=not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid date format. Use ISO YYYY-MM-DD."));
    }

    @Test
    void insuranceValid_returns400ForImpossibleDate() throws Exception {
        when(carService.listCars()).thenReturn(List.of(mockCar()));
        mockMvc.perform(get("/api/cars/1/insurance-valid?date=1800-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Date out of supported range (1900-01-01 to 2100-12-31)."));
    }

    @Test
    void insuranceValid_returnsOkForValidRequest() throws Exception {
        when(carService.listCars()).thenReturn(List.of(mockCar()));
        when(carService.isInsuranceValid(1L, LocalDate.of(2025, 9, 7))).thenReturn(true);

        mockMvc.perform(get("/api/cars/1/insurance-valid?date=2025-09-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carId").value(1))
                .andExpect(jsonPath("$.date").value("2025-09-07"))
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void registerClaim_returns201ForValidClaim() throws Exception {
        Car car = new Car();
        var carIdField = Car.class.getDeclaredField("id");
        carIdField.setAccessible(true);
        carIdField.set(car, 1L);

        when(carService.listCars()).thenReturn(List.of(car));

        // Prepare claim with ID set
        InsuranceClaim claim = new InsuranceClaim();
        var claimIdField = InsuranceClaim.class.getDeclaredField("id");
        claimIdField.setAccessible(true);
        claimIdField.set(claim, 100L);

        // Make the mock return the same claim instance
        when(insuranceClaimRepository.save(any())).thenAnswer(invocation -> {
            InsuranceClaim inputClaim = invocation.getArgument(0);
            var claimIdField1 = InsuranceClaim.class.getDeclaredField("id");
            claimIdField1.setAccessible(true);
            claimIdField1.set(inputClaim, 100L);
            return inputClaim;
        });

        String json = """
            {
                "claimDate": "2025-09-06",
                "description": "Accident",
                "amount": 1200.50
            }
            """;

        mockMvc.perform(post("/api/cars/1/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cars/1/claims/100"));
    }

    @Test
    void registerClaim_returns400ForValidationError() throws Exception {
        String json = """
            {
                "claimDate": "",
                "description": "",
                "amount": null
            }
            """;
        when(carService.listCars()).thenReturn(List.of());

        mockMvc.perform(post("/api/cars/1/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerClaim_returns404ForMissingCar() throws Exception {
        when(carService.listCars()).thenReturn(List.of());

        String json = """
            {
                "claimDate": "2025-09-06",
                "description": "Accident",
                "amount": 1200.50
            }
            """;

        mockMvc.perform(post("/api/cars/999/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Car not found"));
    }

    @Test
    void getCarHistory_returns404ForMissingCar() throws Exception {
        when(carService.listCars()).thenReturn(List.of());

        mockMvc.perform(get("/api/cars/999/history"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Car not found"));
    }

    @Test
    void getCarHistory_returnsChronologicalEvents() throws Exception {
        Car car = mockCar();

        when(carService.listCars()).thenReturn(List.of(car));

        // Mock claims
        InsuranceClaim claim = new InsuranceClaim();
        var claimIdField = InsuranceClaim.class.getDeclaredField("id");
        claimIdField.setAccessible(true);
        claimIdField.set(claim, 1L);
        claim.setCar(car);
        claim.setClaimDate(LocalDate.of(2025, 9, 6));
        claim.setDescription("Accident");
        claim.setAmount(BigDecimal.valueOf(1200.50));

        when(insuranceClaimRepository.findByCarIdOrderByClaimDateAsc(1L)).thenReturn(List.of(claim));

        // Mock policies
        com.example.carins.model.InsurancePolicy policy = new com.example.carins.model.InsurancePolicy();
        var policyIdField = com.example.carins.model.InsurancePolicy.class.getDeclaredField("id");
        policyIdField.setAccessible(true);
        policyIdField.set(policy, 2L);
        policy.setCar(car);
        policy.setStartDate(LocalDate.of(2025, 1, 1));
        policy.setEndDate(LocalDate.of(2025, 12, 31));
        policy.setProvider("ProviderX");

        when(insurancePolicyRepository.findAll()).thenReturn(List.of(policy));

        mockMvc.perform(get("/api/cars/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("POLICY"))
                .andExpect(jsonPath("$[0].startDate").value("2025-01-01"))
                .andExpect(jsonPath("$[0].endDate").value("2025-12-31"))
                .andExpect(jsonPath("$[0].provider").value("ProviderX"))
                .andExpect(jsonPath("$[1].type").value("CLAIM"))
                .andExpect(jsonPath("$[1].date").value("2025-09-06"))
                .andExpect(jsonPath("$[1].description").value("Accident"))
                .andExpect(jsonPath("$[1].amount").value(1200.50));
    }
}
