package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseConduit {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConduit.class);

    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final IncentiveClient incentiveClient;

    public DatabaseConduit(UserRepository userRepository,
                           TransactionRecordRepository transactionRecordRepository,
                           IncentiveClient incentiveClient) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.incentiveClient = incentiveClient;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    /**
     * Validates and records an incoming transaction.
     * Valid = sender exists, recipient exists, sender balance >= amount.
     * On success the transaction is posted to the incentive API; the incentive
     * is added to the recipient's balance (never deducted from the sender)
     * and stored on the TransactionRecord.
     */
    @Transactional
    public boolean processTransaction(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null || recipient == null || sender.getBalance() < transaction.getAmount()) {
            logger.info("Transaction discarded: {}", transaction);
            return false;
        }

        // Only validated transactions earn incentives
        float incentive = incentiveClient.fetchIncentive(transaction);

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive);
        userRepository.save(sender);
        userRepository.save(recipient);
        transactionRecordRepository.save(new TransactionRecord(sender, recipient, transaction.getAmount(), incentive));

        // Balance logging makes test verification possible without a debugger
        logger.info("Transaction recorded: {} incentive={} | balances: {}={}, {}={}",
                transaction, incentive, sender.getName(), sender.getBalance(),
                recipient.getName(), recipient.getBalance());
        return true;
    }
}
