---
id: ADR-005
title: Service Consolidation to Four Core Services
status: Accepted
date: 2026-05-19
---

## Summary

In the context of a nine-service initial architecture with overlapping responsibilities and stub implementations, facing a project timeline that made delivering nine production-ready services impractical, we decided to consolidate to four core application services with clear domain boundaries, to maximise the amount of working, tested functionality delivered, accepting that the stats-service takes on a broader scope than originally planned for a single service.

## Context

The initial architecture had up to nine application services: auth-service, expense-service, group-service, banking-service, search-service, analytics-service, budget-alert-service, alerts-service, and expense-ingest-service. Several of these had overlapping responsibilities, port conflicts, and were entirely stub implementations with no business logic.

## Decision

Consolidate to four core application services:

| Service | Responsibilities |
|---|---|
| auth-service | Register, login via AWS Cognito |
| banking-service | OBP API adapter, account listing, real-time transactions |
| stats-service | Expense CRUD, group/collaborator management, search, dashboard |
| alert-service | Budget CRUD, threshold evaluation, alert management |

The following services are removed:
- `expense-service` -- merged into stats-service
- `group-service` -- merged into stats-service
- `search-service` -- merged into stats-service
- `analytics-service` -- replaced by stats-service (real-time, no persistence)
- `budget-alert-service` + `alerts-service` -- merged into alert-service
- `expense-ingest-service` -- removed (bank transaction import is a direct POST to stats-service from the frontend)
- `api-gateway` -- removed (duplicate of gateway)

## Consequences

**Positive:**
- Reduced deployment surface -- four services instead of nine
- Eliminates port conflicts and the duplicate gateway
- Each service has a clear, cohesive domain boundary
- Simpler Terraform and docker-compose

**Negative:**
- Stats-service handles expenses, groups, search, and the dashboard -- a larger scope than originally intended for a single service
- Individual sub-features within stats-service cannot be scaled independently

Four well-implemented services with tests deliver more value than nine skeleton services given the project timeline and team size. The consolidation is a deliberate scope decision, not a design failure.
