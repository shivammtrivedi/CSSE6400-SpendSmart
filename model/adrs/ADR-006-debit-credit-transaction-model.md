---
id: ADR-006
title: Debit/Credit Model for Bank Transactions
status: Accepted
date: 2026-05-19
---

## Summary

In the context of normalising OBP transaction data for budget calculations and the spending dashboard, facing the inability to distinguish money leaving an account from money entering it with a single amount field, we decided to add a `transactionType` field (DEBIT/CREDIT) to the normalised transaction schema, to ensure budget calculations only count outgoing money and the dashboard can display income and expenses separately.

## Context

The OBP API returns transactions that include both money leaving an account (purchases, payments) and money entering an account (refunds, income, transfers). The initial `NormalisedTransaction` DTO contained only an `amount` field with no indication of transaction direction. This meant refunds and credits could not be distinguished from debits, leading to incorrect budget calculations and spending aggregations.

## Decision

All normalised bank transactions include a `transactionType` field with two possible values:

- `DEBIT` -- money leaving the account (expenses, purchases, payments)
- `CREDIT` -- money entering the account (refunds, income, transfers in)

This is the standard double-entry accounting model used across financial systems. For budget threshold evaluation, only `DEBIT` transactions count against spending limits. For the stats dashboard, both types are available for display but aggregations default to debits only unless explicitly requested.

The OBP adapter maps OBP transaction data to this model: negative `value.amount` maps to `DEBIT`, positive maps to `CREDIT`.

## Consequences

**Positive:**
- Budget calculations are correct -- refunds do not reduce reported spending
- Aligns with standard financial industry conventions
- Stats dashboard can show both income and expenses if needed
- Clean, unambiguous data model

**Negative:**
- Slightly more complex normalisation logic in the OBP adapter
- Frontend must handle both transaction types when displaying data
