package com.financial.transactionservice.service;

import com.financial.transactionservice.client.AccountServiceClient;
import com.financial.transactionservice.dto.AccountResponse;
import com.financial.transactionservice.dto.TransferRequest;
import com.financial.transactionservice.dto.TransferResponse;
import com.financial.transactionservice.entity.Transaction;
import com.financial.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        log.info("Initiating transfer: from={}, to={}, amount={}",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .status("PENDING")
                .build();
        transaction = transactionRepository.save(transaction);

        try {
            // Step 1: Validate accounts exist
            AccountResponse fromAccount = accountServiceClient.getAccount(request.getFromAccountId());
            AccountResponse toAccount = accountServiceClient.getAccount(request.getToAccountId());

            if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance in source account");
            }

            // Step 2: Debit from source account
            log.info("Debiting account {}", request.getFromAccountId());
            accountServiceClient.debit(request.getFromAccountId(),
                    Map.of("amount", request.getAmount()));

            try {
                // Step 3: Credit to destination account
                log.info("Crediting account {}", request.getToAccountId());
                accountServiceClient.credit(request.getToAccountId(),
                        Map.of("amount", request.getAmount()));

                // Success - mark transaction completed
                transaction.setStatus("COMPLETED");
                transaction.setCompletedAt(LocalDateTime.now());
                transactionRepository.save(transaction);

                return TransferResponse.builder()
                        .transactionId(transaction.getId())
                        .status("COMPLETED")
                        .message("Transfer successful")
                        .timestamp(LocalDateTime.now())
                        .build();

            } catch (Exception e) {
                // Credit failed - Compensate: credit back to source account
                log.error("Credit failed, compensating by crediting back to source account", e);
                accountServiceClient.credit(request.getFromAccountId(),
                        Map.of("amount", request.getAmount()));

                transaction.setStatus("COMPENSATED");
                transaction.setFailureReason("Credit to destination failed: " + e.getMessage());
                transactionRepository.save(transaction);

                return TransferResponse.builder()
                        .transactionId(transaction.getId())
                        .status("COMPENSATED")
                        .message("Transfer failed and rolled back")
                        .timestamp(LocalDateTime.now())
                        .build();
            }

        } catch (Exception e) {
            // Debit or validation failed - no compensation needed
            log.error("Transfer failed at debit stage", e);
            transaction.setStatus("FAILED");
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);

            return TransferResponse.builder()
                    .transactionId(transaction.getId())
                    .status("FAILED")
                    .message("Transfer failed: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    public Transaction getTransaction(Long id) {
        return transactionRepository.findById(id).orElse(null);
    }
}