package com.financial.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financial.transactionservice.dto.TransferRequest;
import com.financial.transactionservice.dto.TransferResponse;
import com.financial.transactionservice.entity.Transaction;
import com.financial.transactionservice.repository.TransactionRepository;
import com.financial.transactionservice.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Health endpoint should return UP status")
    void health_ShouldReturnUp() throws Exception {

        mockMvc.perform(get("/api/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("transaction-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Transfer endpoint should return completed response")
    void transfer_ShouldReturnCompleted() throws Exception {

        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("200.00"));

        TransferResponse response = TransferResponse.builder()
                .transactionId(1001L)
                .status("COMPLETED")
                .message("Transfer successful")
                .timestamp(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();

        when(transactionService.transfer(any(TransferRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId", is(1001)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.message", is("Transfer successful")));

        verify(transactionService).transfer(any(TransferRequest.class));
    }

    @Test
    @DisplayName("Get transaction should return transaction details")
    void getTransaction_ShouldReturnTransaction() throws Exception {

        Transaction transaction = Transaction.builder()
                .id(1L)
                .userId(10L)
                .fromAccountId(1L)
                .toAccountId(2L)
                .amount(new BigDecimal("500.00"))
                .status("COMPLETED")
                .build();

        when(transactionService.getTransaction(1L))
                .thenReturn(transaction);

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(500.00));
    }

    @Test
    @DisplayName("Get transaction should return 404 when not found")
    void getTransaction_ShouldReturn404_WhenTransactionNotFound() throws Exception {

        when(transactionService.getTransaction(999L))
                .thenReturn(null);

        mockMvc.perform(get("/api/transactions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Get all transactions should return transaction list")
    void getAllTransactions_ShouldReturnList() throws Exception {

        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .id(1L)
                        .amount(new BigDecimal("100.00"))
                        .status("COMPLETED")
                        .build(),

                Transaction.builder()
                        .id(2L)
                        .amount(new BigDecimal("250.00"))
                        .status("COMPLETED")
                        .build()
        );

        when(transactionRepository.findAll())
                .thenReturn(transactions);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DisplayName("Get transactions by user should return user transactions")
    void getTransactionsByUser_ShouldReturnTransactions() throws Exception {

        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .id(1L)
                        .userId(101L)
                        .amount(new BigDecimal("300.00"))
                        .status("COMPLETED")
                        .build()
        );

        when(transactionRepository.findByUserId(101L))
                .thenReturn(transactions);

        mockMvc.perform(get("/api/transactions/user/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(101));
    }

    @Test
    @DisplayName("Get account transactions should return matching records")
    void getTransactionsByAccount_ShouldReturnTransactions() throws Exception {

        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .id(1L)
                        .fromAccountId(1L)
                        .toAccountId(2L)
                        .amount(new BigDecimal("700.00"))
                        .status("COMPLETED")
                        .build()
        );

        when(transactionRepository.findByFromAccountIdOrToAccountId(1L, 1L))
                .thenReturn(transactions);

        mockMvc.perform(get("/api/transactions/account/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromAccountId").value(1));
    }
}