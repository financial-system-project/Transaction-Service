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
        log.info("Initiating transfer: from={}, to={}, amount={}, category={}",
                request.getFromAccountId(), request.getToAccountId(),
                request.getAmount(), request.getCategory());

        // FIX: Fetch source account first to get userId before saving the transaction
        AccountResponse fromAccount;
        try {
            fromAccount = accountServiceClient.getAccount(request.getFromAccountId());
        } catch (Exception e) {
            log.error("Could not fetch source account {}", request.getFromAccountId(), e);
            Transaction failed = Transaction.builder()
                    .fromAccountId(request.getFromAccountId())
                    .toAccountId(request.getToAccountId())
                    .amount(request.getAmount())
                    .category(request.getCategory() != null ? request.getCategory() : "TRANSFER")
                    .status("FAILED")
                    .failureReason("Source account not found: " + e.getMessage())
                    .build();
            failed = transactionRepository.save(failed);
            return TransferResponse.builder()
                    .transactionId(failed.getId())
                    .status("FAILED")
                    .message("Transfer failed: Source account not found")
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Create transaction record with userId + category so Budget-Service can query it
        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                // FIX: Store the owner's userId for Budget-Service queries
                .userId(fromAccount.getUserId())
                // FIX: Store category (defaults to "TRANSFER" if not sent)
                .category(request.getCategory() != null ? request.getCategory() : "TRANSFER")
                .amount(request.getAmount())
                .status("PENDING")
                .build();
        transaction = transactionRepository.save(transaction);

        try {
            if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance in source account");
            }

            // Step 1: Debit source account
            log.info("Debiting account {}", request.getFromAccountId());
            accountServiceClient.debit(request.getFromAccountId(),
                    Map.of("amount", request.getAmount()));

            try {
                // Step 2: Credit destination account
                log.info("Crediting account {}", request.getToAccountId());
                accountServiceClient.credit(request.getToAccountId(),
                        Map.of("amount", request.getAmount()));

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
                // Credit failed — compensate by crediting back to source
                log.error("Credit failed, compensating debit", e);
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
            log.error("Transfer failed", e);
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