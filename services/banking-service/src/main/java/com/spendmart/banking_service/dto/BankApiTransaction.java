package com.spendmart.banking_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankApiTransaction {

    @JsonProperty("_id")
    private String id;

    private String transactionType;
    private Long accountNumber;
    private String sender;
    private String description;
    private Double transactionAmount;
    private String transactionTime;
}
