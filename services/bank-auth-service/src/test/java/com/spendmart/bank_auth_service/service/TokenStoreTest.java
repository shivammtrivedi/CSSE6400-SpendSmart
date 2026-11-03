package com.spendmart.bank_auth_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenStoreTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private TokenStore tokenStore;

    @BeforeEach
    void setUp() {
        tokenStore = new TokenStore(dynamoDbClient);
        ReflectionTestUtils.setField(tokenStore, "tableName", "bank-auth-tokens");
    }

    @Test
    void put_callsDynamoDbWithCorrectItem() {
        when(dynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(PutItemResponse.builder().build());

        tokenStore.put("user-123", "obp", "test-token");

        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());

        PutItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo("bank-auth-tokens");
        assertThat(request.item().get("userId").s()).isEqualTo("user-123");
        assertThat(request.item().get("provider").s()).isEqualTo("obp");
        assertThat(request.item().get("token").s()).isEqualTo("test-token");
    }

    @Test
    void get_returnsToken_whenItemExists() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(
                GetItemResponse.builder()
                        .item(Map.of("token", AttributeValue.fromS("stored-token")))
                        .build()
        );

        Optional<String> result = tokenStore.get("user-123", "obp");

        assertThat(result).contains("stored-token");
    }

    @Test
    void get_returnsEmpty_whenItemMissing() {
        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(
                GetItemResponse.builder().build()
        );

        Optional<String> result = tokenStore.get("user-123", "obp");

        assertThat(result).isEmpty();
    }
}
