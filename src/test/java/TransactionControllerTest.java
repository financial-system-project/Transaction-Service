package com.financial.transactionservice.controller;

import com.financial.transactionservice.dto.TransferRequest;
import com.financial.transactionservice.dto.TransferResponse;
import com.financial.transactionservice.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_ShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void transfer_ShouldReturnCompleted() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("200.00"));

        TransferResponse response = TransferResponse.builder()
                .transactionId(1L)
                .status("COMPLETED")
                .message("Transfer successful")
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionService.transfer(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }
}