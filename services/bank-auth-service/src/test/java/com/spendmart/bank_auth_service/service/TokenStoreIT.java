package com.spendmart.bank_auth_service.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TokenStoreIT {

    private static final String TABLE = "bank-auth-tokens";

    @Container
    static GenericContainer<?> dynamoDb =
            new GenericContainer<>("amazon/dynamodb-local:2.5.3")
                    .withExposedPorts(8000);

    private static TokenStore tokenStore;

    @BeforeAll
    static void setup() {
        DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:" + dynamoDb.getMappedPort(8000)))
                .region(Region.AP_SOUTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fakeKey", "fakeSecret")))
                .build();

        client.createTable(CreateTableRequest.builder()
                .tableName(TABLE)
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName("userId").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder()
                                .attributeName("provider").keyType(KeyType.RANGE).build()
                )
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("userId").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder()
                                .attributeName("provider").attributeType(ScalarAttributeType.S).build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

        tokenStore = new TokenStore(client);
        ReflectionTestUtils.setField(tokenStore, "tableName", TABLE);
    }

    @Test
    void put_andGet_roundTrip() {
        tokenStore.put("user-123", "obp", "tok_abc");
        assertThat(tokenStore.get("user-123", "obp")).contains("tok_abc");
    }

    @Test
    void get_returnsEmpty_whenKeyAbsent() {
        assertThat(tokenStore.get("nonexistent", "obp")).isEmpty();
    }

    @Test
    void put_overwritesExistingToken() {
        tokenStore.put("user-456", "obp", "tok_old");
        tokenStore.put("user-456", "obp", "tok_new");
        assertThat(tokenStore.get("user-456", "obp")).contains("tok_new");
    }
}
