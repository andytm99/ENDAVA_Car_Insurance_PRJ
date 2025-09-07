package com.example.carins.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.carins.repo.InsurancePolicyRepository;

@Service
public class PolicyExpiryLogger {

    private static final Logger log = LoggerFactory.getLogger(PolicyExpiryLogger.class);
    private final InsurancePolicyRepository policyRepo;
    private final Set<Long> loggedPolicies = new HashSet<>();

    public PolicyExpiryLogger(InsurancePolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void logExpiredPolicies() {
        LocalDate now = LocalDate.now();
        policyRepo.findAll().forEach(policy -> {
            if (!loggedPolicies.contains(policy.getId())
                    && policy.getEndDate() != null
                    && policy.getEndDate().isEqual(now.minusDays(1))) {
                log.info("Policy {} for car {} expired on {}", policy.getId(), policy.getCar().getId(), policy.getEndDate());
                loggedPolicies.add(policy.getId());
            }
        });
    }
}
