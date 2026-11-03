package com.spendmart.banking_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.spendmart.banking_service.adapter.BankApiAdapter;
import com.spendmart.banking_service.adapter.ObpAdapter;
import com.spendmart.banking_service.client.BankApiNodeJsClient;
import com.spendmart.banking_service.client.BankAuthClient;
import com.spendmart.banking_service.client.ObpApiClient;
import com.spendmart.banking_service.dto.NormalisedTransaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankingServiceIT {

    static WireMockServer bankAuthWm;
    static WireMockServer obpWm;
    static BankingService bankingService;

    @BeforeAll
    static void setup() {
        bankAuthWm = new WireMockServer(wireMockConfig().dynamicPort());
        obpWm = new WireMockServer(wireMockConfig().dynamicPort());
        bankAuthWm.start();
        obpWm.start();

        BankAuthClient bankAuthClient = new BankAuthClient(
                RestClient.builder().baseUrl("http://localhost:" + bankAuthWm.port()).build());

        ObpApiClient obpApiClient = new ObpApiClient(
                RestClient.builder().baseUrl("http://localhost:" + obpWm.port()).build());
        ReflectionTestUtils.setField(obpApiClient, "apiVersion", "v5.1.0");

        ObpAdapter obpAdapter = new ObpAdapter(obpApiClient);
        BankApiNodeJsClient bankApiNodeJsClient = new BankApiNodeJsClient(
                RestClient.builder().baseUrl("http://localhost:" + obpWm.port()).build(),
                new ObjectMapper());
        BankApiAdapter bankApiAdapter = new BankApiAdapter(bankApiNodeJsClient);

        bankingService = new BankingService(List.of(obpAdapter, bankApiAdapter), bankAuthClient);
    }

    @AfterAll
    static void teardown() {
        bankAuthWm.stop();
        obpWm.stop();
    }

    @BeforeEach
    void resetStubs() {
        bankAuthWm.resetAll();
        obpWm.resetAll();
    }

    @Test
    void getTransactions_returnsMappedList_whenBothServicesRespond() {
        bankAuthWm.stubFor(get(urlEqualTo("/api/v1/token/obp"))
                .willReturn(okJson("{\"token\":\"obp-tok-xyz\"}")));
        obpWm.stubFor(get(urlPathMatching("/v5.1.0/my/transactions"))
                .willReturn(okJson("""
                        {
                          "transactions": [{
                            "id": "txn-001",
                            "this_account": {"id": "acc-1", "bank_id": "bnk-1"},
                            "details": {
                              "type": "DEBIT",
                              "description": "Coffee shop",
                              "posted": "2026-05-30T10:00:00Z",
                              "value": {"currency": "AUD", "amount": "-5.50"}
                            }
                          }]
                        }
                        """)));

        List<NormalisedTransaction> result = bankingService.getTransactions("user-1", "fake-jwt");

        assertThat(result).hasSize(1);
        NormalisedTransaction tx = result.get(0);
        assertThat(tx.getId()).isNotNull();
        assertThat(tx.getExternalTxnId()).isEqualTo("txn-001");
        assertThat(tx.getProvider()).isEqualTo("obp");
        assertThat(tx.getUserId()).isEqualTo("user-1");
        assertThat(tx.getCurrency()).isEqualTo("AUD");
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("5.50"));
        assertThat(tx.getOccurredAt()).hasToString("2026-05-30T10:00");
    }

    @Test
    void getTransactions_returnsEmptyList_whenObpHasNoTransactions() {
        bankAuthWm.stubFor(get(urlEqualTo("/api/v1/token/obp"))
                .willReturn(okJson("{\"token\":\"obp-tok-xyz\"}")));
        obpWm.stubFor(get(urlPathMatching("/v5.1.0/my/transactions"))
                .willReturn(okJson("{\"transactions\":[]}")));

        assertThat(bankingService.getTransactions("user-1", "fake-jwt")).isEmpty();
    }

    @Test
    void getTransactions_returnsEmptyList_whenNoBankConnected() {
        bankAuthWm.stubFor(get(urlPathMatching("/api/v1/token/.*"))
                .willReturn(notFound()));

        assertThat(bankingService.getTransactions("user-1", "fake-jwt")).isEmpty();
    }

    @Test
    void getTransactions_throws503_whenBankAuthIsUnreachable() {
        bankAuthWm.stubFor(get(urlEqualTo("/api/v1/token/obp"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> bankingService.getTransactions("user-1", "fake-jwt"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
