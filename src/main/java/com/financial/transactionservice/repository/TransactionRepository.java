package com.financial.transactionservice.repository;

import com.financial.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);

    // FIX: Used by GET /api/transactions/account/{id}
    List<Transaction> findByFromAccountId(Long fromAccountId);
    // FIX: Used by Budget-Service to fetch per-user transactions
    List<Transaction> findByUserId(Long userId);
    // FIX: Used by Budget-Service to fetch spending in a given month
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
            "AND t.status = 'COMPLETED' " +
            "AND t.createdAt >= :from AND t.createdAt < :to")
    List<Transaction> findCompletedByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}