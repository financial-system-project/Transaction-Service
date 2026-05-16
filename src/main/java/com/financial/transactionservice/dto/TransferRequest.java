package com.financial.transactionservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    // FIX: Optional category tag — lets callers label the transfer (e.g. "FOOD", "RENT")
    // Defaults to "TRANSFER" in Transaction.onCreate() if not provided
    private String category;
}