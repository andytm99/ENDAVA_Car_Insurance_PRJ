package com.example.carins.service;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import com.example.carins.model.Car;
import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.InsurancePolicyRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class PolicyExpiryLoggerTest {

    @Test
    void logsOnceForExpiredPolicy() {
        // Arrange: mock repository and policy
        InsurancePolicyRepository repo = Mockito.mock(InsurancePolicyRepository.class);
        InsurancePolicy policy = new InsurancePolicy();
        // Set policy ID using reflection
        try {
            var field = InsurancePolicy.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(policy, 42L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Car car = new Car();
        // Set car ID using reflection
        try {
            var carIdField = Car.class.getDeclaredField("id");
            carIdField.setAccessible(true);
            carIdField.set(car, 7L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        policy.setCar(car);
        policy.setEndDate(LocalDate.now().minusDays(1)); // expired yesterday
        Mockito.when(repo.findAll()).thenReturn(List.of(policy));

        PolicyExpiryLogger loggerService = new PolicyExpiryLogger(repo);

        // Attach a ListAppender to capture logs
        Logger logger = (Logger) LoggerFactory.getLogger(PolicyExpiryLogger.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        // Act: run the logger twice
        loggerService.logExpiredPolicies(); // Should log
        loggerService.logExpiredPolicies(); // Should NOT log again

        // Assert: only one log entry
        List<ILoggingEvent> logs = appender.list;
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getFormattedMessage())
                .contains("Policy 42 for car 7 expired on " + policy.getEndDate());
    }
}
