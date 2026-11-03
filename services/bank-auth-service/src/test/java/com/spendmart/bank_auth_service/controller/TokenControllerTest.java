package com.spendmart.bank_auth_service.controller;

import com.spendmart.bank_auth_service.config.SecurityConfig;
import com.spendmart.bank_auth_service.service.TokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TokenController.class)
@Import(SecurityConfig.class)
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenStore tokenStore;

    @Test
    void getToken_returns200WithToken_whenObpTokenExists() throws Exception {
        when(tokenStore.get("user-123", "obp")).thenReturn(Optional.of("obp-token-abc"));

        mockMvc.perform(get("/api/v1/token/obp")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("obp-token-abc"));
    }

    @Test
    void getToken_returns200WithToken_whenBankApiTokenExists() throws Exception {
        when(tokenStore.get("user-123", "bankapi")).thenReturn(Optional.of("ba-token-xyz"));

        mockMvc.perform(get("/api/v1/token/bankapi")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("ba-token-xyz"));
    }

    @Test
    void getToken_returns404_whenNoTokenStored() throws Exception {
        when(tokenStore.get("user-123", "obp")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/token/obp")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getToken_returns401_whenNoJwt() throws Exception {
        mockMvc.perform(get("/api/v1/token/obp"))
                .andExpect(status().isUnauthorized());
    }
}
