package com.financial.transactionservice.client;

import com.financial.transactionservice.dto.AccountResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "account-service", url = "${account.service.url:http://account-service:8081}")
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{id}")
    AccountResponse getAccount(@PathVariable("id") Long id);

    @PostMapping("/api/accounts/{id}/debit")
    AccountResponse debit(@PathVariable("id") Long id, @RequestBody Map<String, BigDecimal> request);

    @PostMapping("/api/accounts/{id}/credit")
    AccountResponse credit(@PathVariable("id") Long id, @RequestBody Map<String, BigDecimal> request);
}