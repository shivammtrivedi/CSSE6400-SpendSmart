---
id: ADR-013
title: BankApiNodeJs as OBP Fallback for MVP
status: Accepted
date: 2026-06-05
---

## Summary

In the context of needing live banking data for the MVP demo, facing the inability to obtain an OBP API key due to unresponsive support, we decided to include BankApiNodeJs as the primary banking data source for the MVP, accepting that the direct OBP integration remains incomplete.

## Context

- The architecture relies on OBP (Open Bank Project) for real bank transaction data (see ADR-004)
- Obtaining a production OBP API key requires approval from the OBP team
- The OBP support team was unresponsive during the project timeline, and a usable API key was not obtained in time for the MVP

## Decision

BankApiNodeJs, a Node.js service that simulates the banking API, is included in the deployment as a fallback banking data source for the MVP. The ALB catch-all rule routes `/api/*` to this service (see ADR-010). The direct OBP integration path remains in the codebase but is not exercisable without a valid OBP API key.

## Consequences

**Positive:**
- The MVP can demonstrate banking data features end-to-end without a live OBP connection
- The rest of the architecture (banking-service, stats-service, alert-service) is unchanged

**Negative:**
- Banking data shown in the MVP is simulated, not live
- The OBP integration is unverified against real bank data
