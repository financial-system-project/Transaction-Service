package com.financial.transactionservice.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransferResponse {
    private Long transactionId;
    private String status;
    private String message;
    private LocalDateTime timestamp;
}