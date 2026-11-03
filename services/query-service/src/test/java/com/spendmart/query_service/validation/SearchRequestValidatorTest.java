package com.spendmart.query_service.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.spendmart.query_service.dto.TransactionFilter;
//claude was used to update the tests to reflect the changes from using the banking stub +  updated fields 
// in NormalisedTransaction
class SearchRequestValidatorTest {

    private final SearchRequestValidator validator = new SearchRequestValidator();

    @Test
    void shouldReturnNoErrorsForNullFilter() {
        List<String> errors = validator.validate(null);
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldReturnNoErrorsForEmptyFilter() {
        List<String> errors = validator.validate(new TransactionFilter());
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldDetectMinAmountGreaterThanMaxAmount() {
        TransactionFilter filter = new TransactionFilter();
        filter.setMinAmount(new BigDecimal("100"));
        filter.setMaxAmount(new BigDecimal("50"));

        List<String> errors = validator.validate(filter);
        assertTrue(errors.contains("minAmount must not be greater than maxAmount"));
    }

    @Test
    void shouldAcceptValidAmountRange() {
        TransactionFilter filter = new TransactionFilter();
        filter.setMinAmount(new BigDecimal("10"));
        filter.setMaxAmount(new BigDecimal("100"));

        List<String> errors = validator.validate(filter);
        assertFalse(errors.contains("minAmount must not be greater than maxAmount"));
    }

    @Test
    void shouldDetectFromDateAfterToDate() {
        TransactionFilter filter = new TransactionFilter();
        filter.setFromDate(LocalDate.of(2026, 6, 10));
        filter.setToDate(LocalDate.of(2026, 6, 1));

        List<String> errors = validator.validate(filter);
        assertTrue(errors.contains("fromDate must not be after toDate"));
    }

    @Test
    void shouldAcceptValidDateRange() {
        TransactionFilter filter = new TransactionFilter();
        filter.setFromDate(LocalDate.of(2026, 6, 1));
        filter.setToDate(LocalDate.of(2026, 6, 10));

        List<String> errors = validator.validate(filter);
        assertFalse(errors.contains("fromDate must not be after toDate"));
    }

}