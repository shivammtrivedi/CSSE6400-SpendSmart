package com.spendmart.bank_auth_service.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spendmart.bank_auth_service.dto.ObpLoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ObpDirectLoginClient {

    private final RestClient obpAuthRestClient;

    public String login(ObpLoginRequest request) {
        String directLoginHeader = String.format(
                "DirectLogin username=\"%s\",password=\"%s\",consumer_key=\"%s\"",
                request.username(), request.password(), request.consumerKey()
        );

        DirectLoginResponse response = obpAuthRestClient.post()
                .uri("/my/logins/direct")
                .header("Authorization", directLoginHeader)
                .retrieve()
                .body(DirectLoginResponse.class);

        if (response == null || response.token() == null) {
            throw new IllegalStateException("OBP DirectLogin returned no token");
        }
        return response.token();
    }

    private record DirectLoginResponse(@JsonProperty("token") String token) {}
}
