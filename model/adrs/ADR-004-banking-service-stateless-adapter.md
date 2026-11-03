---
id: ADR-004
title: Banking Service as Stateless Real-Time OBP Adapter
status: Accepted
date: 2026-05-19
---

## Summary

In the context of providing transaction data to the stats dashboard and budget alert evaluation, facing the complexity of maintaining a local cache of OBP transactions, we decided to make the banking service completely stateless with no database, fetching live from OBP on every request and returning normalised transactions, to eliminate sync logic and a dedicated database, accepting that OBP availability is now a hard dependency for dashboard and budget features.

## Context

The initial banking service design cached OBP transactions locally in a `bank_transactions` PostgreSQL table, with a sync endpoint to periodically pull from OBP and store results. This required a dedicated database, deduplication logic via `externalId`, and a separate sync trigger.

The stats and alert services were planned to query this local cache rather than OBP directly.

## Decision

The banking service is stateless -- it holds no database and performs no local caching. Every request to the banking service results in a live call to the OBP API. The service's only responsibilities are authentication with OBP, making the API call, and normalising the OBP response into the internal `NormalisedTransaction` schema. Stats and alert services call the banking service directly via REST whenever they need transaction data.

## Consequences

**Positive:**
- No banking database to provision or maintain
- Always returns live data, no stale cache to worry about
- Banking service is horizontally scalable with no shared state
- Simpler codebase -- no sync logic, no deduplication

**Negative:**
- Dashboard and budget checks are entirely dependent on OBP availability -- if OBP is down, these features fail
- OBP response time directly affects dashboard load time
- Risk of hitting OBP rate limits under heavy usage

For a university MVP with a small user base, OBP availability is acceptable as a dependency. The simplicity of a stateless adapter outweighs the resilience benefits of local caching. OBP unavailability is documented as a known single point of failure.
