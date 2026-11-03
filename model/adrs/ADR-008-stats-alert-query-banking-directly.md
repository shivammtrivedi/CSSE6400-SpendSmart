---
id: ADR-008
title: Stats and Alert Services Query Banking Service Directly via REST
status: Accepted
date: 2026-05-19
---

## Summary

In the context of the stats dashboard and budget alert evaluation both needing bank transaction data, facing the rejection of Redis Streams and a local banking cache (see ADR-001, ADR-004), we decided to have both services call the banking service directly via synchronous REST on demand, to avoid any intermediate data store and keep the call chain simple, accepting that both features fail if the banking service or OBP is unavailable.

## Context

The stats service needs transaction data to build the spending dashboard. The alert service needs transaction data to evaluate whether spending has exceeded a budget threshold. Originally this data flow was planned via Redis Streams (async event-driven), then via a local cache in the banking service database.

Both approaches were rejected (see ADR-001, ADR-004).

## Decision

Both stats-service and alert-service call the banking service directly via synchronous REST whenever they need transaction data. There is no intermediary cache or message broker. The banking service fetches live from OBP and returns normalised transactions on every call.

- Stats-service calls `GET /api/banking/transactions` to populate the dashboard
- Alert-service calls `GET /api/banking/transactions` when evaluating budget thresholds, summing only DEBIT transactions for the relevant period and category

## Consequences

**Positive:**
- Data is always live -- no staleness between OBP and the dashboard or budget check
- Simple call chain: frontend -> stats -> banking -> OBP
- No additional infrastructure

**Negative:**
- Both stats and alert services have a hard dependency on banking service availability
- Every dashboard load and budget check incurs an OBP API round trip
- If OBP is slow, both the dashboard and alert evaluation are affected
- Redundant OBP calls are possible if stats and alert both request transactions for the same user in a short window

Live data and simplicity are prioritised over resilience for this MVP. Circuit breakers and response caching are noted as future improvements in the project reflection.
