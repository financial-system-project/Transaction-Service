package com.financial.transactionservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fromAccountId;
    private Long toAccountId;

    // FIX: Added so Budget-Service can query transactions per user
    private Long userId;

    // FIX: Added so Budget-Service can aggregate spending per category
    private String category;

    private BigDecimal amount;
    private String status; // PENDING, COMPLETED, FAILED, COMPENSATED
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (category == null) category = "TRANSFER";
    }
}