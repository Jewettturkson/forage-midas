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

    public DatabaseConduit(UserRepository userRepository,
                           TransactionRecordRepository transactionRecordRepository) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }

    public void save(UserRecord userRecord) {
        userRepository.save(userRecord);
    }

    /**
     * Validates and records an incoming transaction.
     * Valid = sender exists, recipient exists, sender balance >= amount.
     * On success both balances are adjusted and a TransactionRecord is stored;
     * otherwise the transaction is discarded with no database changes.
     * Transactional so a partial update can never be persisted.
     */
    @Transactional
    public boolean processTransaction(Transaction transaction) {
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null || recipient == null || sender.getBalance() < transaction.getAmount()) {
            logger.info("Transaction discarded: {}", transaction);
            return false;
        }

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount());
        userRepository.save(sender);
        userRepository.save(recipient);
        transactionRecordRepository.save(new TransactionRecord(sender, recipient, transaction.getAmount()));

        // Balance logging makes test verification possible without a debugger
        logger.info("Transaction recorded: {} | balances: {}={}, {}={}",
                transaction, sender.getName(), sender.getBalance(),
                recipient.getName(), recipient.getBalance());
        return true;
    }
}
