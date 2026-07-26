package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the Kafka topic configured under general.kafka-topic and
 * deserializes each incoming message into a Transaction.
 * Task 2 only requires receiving the transactions; persistence comes later.
 */
@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    // Topic is injected from application.yml so config stays out of code
    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core")
    public void listen(Transaction transaction) {
        // Logging the amount makes the answer visible without a debugger
        logger.info("Received transaction: {}", transaction);
    }
}
