package com.spendmart.query_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

/**
 * DTO representing the filter criteria for querying transactions.
 * Maps directly to NormalisedTransaction fields for filtering.
 */
@Data
public class TransactionFilter {


    private String provider;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;


    private String currency;
}
