---
id: ADR-001
title: Synchronous REST Communication, No Message Queues or Event Streams
status: Accepted
date: 2026-05-19
---

## Summary

In the context of propagating expense changes to the alert service for budget threshold checking, facing the need to keep infrastructure manageable for a small team on a university project, we decided to use synchronous REST calls for all inter-service communication and drop Kafka and Redis Streams from the design, to avoid the operational overhead of running message brokers, accepting that services are now tightly coupled and a failed alert service will affect expense writes.

## Context

Early designs included Apache Kafka and later Redis Streams to handle asynchronous event-driven communication between services (for example, the expense-service publishing events consumed by analytics and budget-alert services). The team also considered BullMQ (Node.js job queue) as an alternative.

The system needs to propagate expense changes to the alert service for budget threshold checking and surface transaction data for the stats dashboard.

## Decision

All inter-service communication uses synchronous REST calls. No message brokers, queues, or event streams are used anywhere in the architecture. When an expense is created or updated, the stats service calls the alert service directly via REST to trigger budget threshold evaluation.

## Consequences

**Positive:**
- Simpler infrastructure -- no Redis, no Kafka, no ElastiCache to provision or maintain
- Request flows are easy to reason about -- each operation is a direct call with an immediate response
- Fewer moving parts reduces deployment complexity and cost
- Testable with standard HTTP integration tests

**Negative:**
- The stats service and alert service are synchronously coupled -- if the alert service is slow or unavailable, expense operations are affected
- No guaranteed delivery -- if the alert service is down during an expense write, the threshold check is missed entirely
- Fanning out to multiple consumers in future would require refactoring

At university scale the operational simplicity gained here outweighs the decoupling benefits of an event-driven approach. The tight coupling is acknowledged as a known limitation.
