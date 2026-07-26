package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for the external incentive API. Spring handles JSON
 * (de)serialization of the Transaction request and Incentive response.
 */
@Component
public class IncentiveClient {
    private static final Logger logger = LoggerFactory.getLogger(IncentiveClient.class);

    private final RestTemplate restTemplate;
    private final String incentiveUrl;

    public IncentiveClient(RestTemplateBuilder builder,
                           @Value("${general.incentive-api:http://localhost:8080/incentive}") String incentiveUrl) {
        this.restTemplate = builder.build();
        this.incentiveUrl = incentiveUrl;
    }

    public float fetchIncentive(Transaction transaction) {
        try {
            Incentive incentive = restTemplate.postForObject(incentiveUrl, transaction, Incentive.class);
            return incentive == null ? 0f : incentive.getAmount();
        } catch (Exception e) {
            // Incentives are a bonus, not a requirement — don't block the
            // transaction if the API is down; treat it as no incentive.
            logger.warn("Incentive API call failed, defaulting to 0: {}", e.getMessage());
            return 0f;
        }
    }
}
