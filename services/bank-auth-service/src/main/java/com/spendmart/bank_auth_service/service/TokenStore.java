package com.spendmart.bank_auth_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenStore {

    private final DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table-name}")
    private String tableName;

    public void put(String userId, String provider, String token) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "userId",   AttributeValue.fromS(userId),
                        "provider", AttributeValue.fromS(provider),
                        "token",    AttributeValue.fromS(token)
                ))
                .build());
    }

    public Optional<String> get(String userId, String provider) {
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "userId",   AttributeValue.fromS(userId),
                        "provider", AttributeValue.fromS(provider)
                ))
                .build());

        if (!response.hasItem() || !response.item().containsKey("token")) {
            return Optional.empty();
        }
        return Optional.of(response.item().get("token").s());
    }
}
