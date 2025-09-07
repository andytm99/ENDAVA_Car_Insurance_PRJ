package com.example.carins.web;

import com.example.carins.model.Car;
import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.web.dto.InsurancePolicyDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsurancePolicyController.class)
class InsurancePolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InsurancePolicyRepository policyRepo;

    @MockBean
    private CarRepository carRepo;

    @Test
    void createPolicy_returnsOkForValidRequest() throws Exception {
        Car car = new Car();
        var carIdField = Car.class.getDeclaredField("id");
        carIdField.setAccessible(true);
        carIdField.set(car, 1L);

        when(carRepo.findById(1L)).thenReturn(Optional.of(car));
        when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String json = """
            {
                "carId": 1,
                "provider": "ProviderX",
                "startDate": "2025-01-01",
                "endDate": "2025-12-31"
            }
            """;

        mockMvc.perform(post("/api/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("ProviderX"))
                .andExpect(jsonPath("$.startDate").value("2025-01-01"))
                .andExpect(jsonPath("$.endDate").value("2025-12-31"));
    }

    @Test
    void createPolicy_returns400ForMissingCar() throws Exception {
        when(carRepo.findById(99L)).thenReturn(Optional.empty());

        String json = """
            {
                "carId": 99,
                "provider": "ProviderX",
                "startDate": "2025-01-01",
                "endDate": "2025-12-31"
            }
            """;

        mockMvc.perform(post("/api/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Car not found"));
    }

    @Test
    void updatePolicy_returnsOkForValidRequest() throws Exception {
        Car car = new Car();
        var carIdField = Car.class.getDeclaredField("id");
        carIdField.setAccessible(true);
        carIdField.set(car, 1L);

        InsurancePolicy policy = new InsurancePolicy();
        var policyIdField = InsurancePolicy.class.getDeclaredField("id");
        policyIdField.setAccessible(true);
        policyIdField.set(policy, 10L);

        when(policyRepo.findById(10L)).thenReturn(Optional.of(policy));
        when(carRepo.findById(1L)).thenReturn(Optional.of(car));
        when(policyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String json = """
            {
                "carId": 1,
                "provider": "ProviderY",
                "startDate": "2026-01-01",
                "endDate": "2026-12-31"
            }
            """;

        mockMvc.perform(put("/api/policies/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("ProviderY"))
                .andExpect(jsonPath("$.startDate").value("2026-01-01"))
                .andExpect(jsonPath("$.endDate").value("2026-12-31"));
    }

    @Test
    void updatePolicy_returns404ForMissingPolicy() throws Exception {
        when(policyRepo.findById(99L)).thenReturn(Optional.empty());

        String json = """
            {
                "carId": 1,
                "provider": "ProviderY",
                "startDate": "2026-01-01",
                "endDate": "2026-12-31"
            }
            """;

        mockMvc.perform(put("/api/policies/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePolicy_returns400ForMissingCar() throws Exception {
        InsurancePolicy policy = new InsurancePolicy();
        var policyIdField = InsurancePolicy.class.getDeclaredField("id");
        policyIdField.setAccessible(true);
        policyIdField.set(policy, 10L);

        when(policyRepo.findById(10L)).thenReturn(Optional.of(policy));
        when(carRepo.findById(99L)).thenReturn(Optional.empty());

        String json = """
            {
                "carId": 99,
                "provider": "ProviderY",
                "startDate": "2026-01-01",
                "endDate": "2026-12-31"
            }
            """;

        mockMvc.perform(put("/api/policies/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Car not found"));
    }
}
