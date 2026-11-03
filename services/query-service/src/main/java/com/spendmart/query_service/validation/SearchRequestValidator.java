package com.spendmart.query_service.validation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.spendmart.query_service.dto.TransactionFilter;

@Component
public class SearchRequestValidator {

    public List<String> validate(TransactionFilter filter) {
        List<String> errors = new ArrayList<>();

        if (filter == null) {
            return errors;
        }

        if (filter.getMinAmount() != null && filter.getMaxAmount() != null
                && filter.getMinAmount().compareTo(filter.getMaxAmount()) > 0) {
            errors.add("minAmount must not be greater than maxAmount");
        }

        if (filter.getFromDate() != null && filter.getToDate() != null
                && filter.getFromDate().isAfter(filter.getToDate())) {
            errors.add("fromDate must not be after toDate");
        }


        return errors;
    }
}