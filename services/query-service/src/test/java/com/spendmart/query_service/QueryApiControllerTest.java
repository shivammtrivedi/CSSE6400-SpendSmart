package com.spendmart.query_service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.spendmart.query_service.controller.QueryController;
import com.spendmart.query_service.dto.TransactionResult;
import com.spendmart.query_service.repository.CanonicalTransactionRepository;
import com.spendmart.query_service.service.TransactionQueryService;
import com.spendmart.query_service.validation.SearchRequestValidator;

//claude was used to update the tests to reflect the changes from using the banking stub +  updated fields 
// in NormalisedTransaction

@WebMvcTest(QueryController.class)
@Import({SearchRequestValidator.class, com.spendmart.query_service.config.SecurityConfig.class})
class QueryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionQueryService transactionQueryService;

    @MockitoBean
    private CanonicalTransactionRepository canonicalTransactionRepository;

    private TransactionResult sampleTransaction() {
        TransactionResult t = new TransactionResult();
        t.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        t.setUserId("user");
        t.setExternalTxnId("ext-001");
        t.setProvider("obp");
        t.setAmount(new BigDecimal("150.00"));
        t.setCurrency("AUD");
        t.setOccurredAt(LocalDateTime.of(2026, 5, 1, 9, 30));
        return t;
    }

    @Test
    void shouldReturnTransactions() throws Exception {
        when(transactionQueryService.queryTransactions(eq("user"), any(), any()))
                .thenReturn(List.of(sampleTransaction()));

        mockMvc.perform(get("/api/v1/query/transactions")
                        .with(jwt().jwt(builder -> builder.subject("user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].externalTxnId").value("ext-001"))
                .andExpect(jsonPath("$[0].provider").value("obp"))
                .andExpect(jsonPath("$[0].currency").value("AUD"));
    }

    @Test
    void shouldReturnEmptyListWhenNoTransactions() throws Exception {
        when(transactionQueryService.queryTransactions(eq("user"), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/query/transactions")
                        .with(jwt().jwt(builder -> builder.subject("user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    void shouldReturn400WhenMinAmountExceedsMaxAmount() throws Exception {
        mockMvc.perform(get("/api/v1/query/transactions")
                        .with(jwt().jwt(builder -> builder.subject("user")))
                        .param("minAmount", "500")
                        .param("maxAmount", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenFromDateAfterToDate() throws Exception {
        mockMvc.perform(get("/api/v1/query/transactions")
                        .with(jwt().jwt(builder -> builder.subject("user")))
                        .param("fromDate", "2026-06-10")
                        .param("toDate", "2026-06-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/query/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200ForHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/query/health"))
                .andExpect(status().isOk());
    }
}
