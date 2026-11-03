---
id: ADR-002
title: Single Shared PostgreSQL Database for All Services
status: Accepted
date: 2026-05-19
---

## Summary

In the context of a service-based architecture where each service owns its own domain tables, facing the cost and operational overhead of provisioning separate database instances per service, we decided to share a single PostgreSQL instance across all services with each service accessing only its own tables, to reduce infrastructure cost and simplify deployment, accepting that a single database outage takes down all services simultaneously.

## Context

The initial architecture provisioned a separate PostgreSQL database per service (auth_db, expense_db, banking_db, search_db, analytics_db, budget_db) following strict per-service database isolation. This required six separate RDS instances or six databases, adding infrastructure cost and operational overhead.

## Decision

All services share a single PostgreSQL instance (one RDS deployment). Each service owns its own tables within that instance and does not query another service's tables directly -- cross-service data access goes through REST APIs, not direct DB queries. This aligns with a service-based architecture, where shared database infrastructure is expected and the services are logically separated by domain rather than physically isolated by separate data stores.

## Consequences

**Positive:**
- One RDS instance to provision, back up, and monitor
- Simpler Terraform configuration
- Lower cost for a university project deployment
- Single Flyway migration path

**Negative:**
- All services compete for the same database connection pool
- A single DB outage takes down all services at once
- Harder to migrate individual services to separate databases later if the need arises

Logical separation is maintained by convention: each service only accesses its own tables. There is no DB-level enforcement (separate schemas with access controls), so this relies on team discipline to uphold.
