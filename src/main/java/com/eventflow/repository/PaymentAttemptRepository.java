package com.eventflow.repository;

import com.eventflow.model.PaymentAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PaymentAttemptRepository extends MongoRepository<PaymentAttempt, String> {
    Optional<PaymentAttempt> findByTransactionId(String transactionId);

}
