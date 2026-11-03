package com.spendmart.banking_service.controller;

import com.spendmart.banking_service.config.SecurityConfig;
import com.spendmart.banking_service.dto.NormalisedTransaction;
import com.spendmart.banking_service.service.BankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankingController.class)
@Import(SecurityConfig.class)
class BankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankingService bankingService;

    private NormalisedTransaction sampleTransaction() {
        NormalisedTransaction t = new NormalisedTransaction();
        t.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        t.setUserId("user-123");
        t.setExternalTxnId("txn-1");
        t.setProvider("obp");
        t.setAmount(new BigDecimal("42.50"));
        t.setCurrency("AUD");
        t.setOccurredAt(LocalDateTime.of(2024, 3, 15, 10, 0));
        return t;
    }

    @Test
    void getTransactions_returns200WithList() throws Exception {
        when(bankingService.getTransactions(eq("user-123"), any())).thenReturn(List.of(sampleTransaction()));

        mockMvc.perform(get("/api/v1/banking/transactions")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalTxnId").value("txn-1"))
                .andExpect(jsonPath("$[0].provider").value("obp"))
                .andExpect(jsonPath("$[0].amount").value(42.50))
                .andExpect(jsonPath("$[0].currency").value("AUD"));
    }

    @Test
    void getTransactions_returns401_withoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/banking/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTransaction_returns200_whenFound() throws Exception {
        when(bankingService.getTransaction(eq("txn-1"), eq("user-123"), any())).thenReturn(sampleTransaction());

        mockMvc.perform(get("/api/v1/banking/transactions/txn-1")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalTxnId").value("txn-1"));
    }

    @Test
    void getTransaction_returns404_whenNotFound() throws Exception {
        when(bankingService.getTransaction(eq("bad-id"), eq("user-123"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));

        mockMvc.perform(get("/api/v1/banking/transactions/bad-id")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void health_returns200_withoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/banking/health"))
                .andExpect(status().isOk());
    }
}
