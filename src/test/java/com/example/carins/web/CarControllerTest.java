package com.example.carins.web;

import com.example.carins.model.Car;
import com.example.carins.service.CarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CarController.class)
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CarService carService;

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
}
