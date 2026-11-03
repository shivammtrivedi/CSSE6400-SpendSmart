package com.spendmart.banking_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// Maps the raw OBP API JSON response, fields match OBP v5.1.0 transaction schema
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObpTransactionResponse {

    private String id;

    @JsonProperty("this_account")
    private ObpAccount thisAccount;

    private ObpTransactionDetails details;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObpAccount {
        private String id;
        @JsonProperty("bank_id")
        private String bankId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObpTransactionDetails {
        private String type;
        private String description;
        @JsonProperty("posted")
        private String postedDate;
        @JsonProperty("value")
        private ObpValue value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObpValue {
        private String currency;
        private String amount;
    }
}
