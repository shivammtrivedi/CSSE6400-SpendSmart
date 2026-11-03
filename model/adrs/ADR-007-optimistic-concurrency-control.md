---
id: ADR-007
title: Optimistic Concurrency Control for Concurrent Expense Edits
status: Accepted
date: 2026-05-19
---

## Summary

In the context of satisfying the Reliability quality attribute that requires resolving concurrent update conflicts during group expense edits, facing the risk of silently lost updates when multiple collaborators edit the same expense simultaneously, we decided to use JPA optimistic locking via `@Version` on the Expense entity, to detect conflicts and return HTTP 409 so clients can retry with fresh data, accepting that clients must implement retry logic.

## Context

The spec's Reliability quality attribute requires the system to "resolve concurrent update conflicts during group expense edits." Multiple collaborators within a group may attempt to update the same expense simultaneously. Without a concurrency control mechanism, the last write silently overwrites earlier writes -- data is consistent but updates are lost without any notification.

## Decision

Use JPA optimistic locking via the `@Version` annotation on the `Expense` entity. A `version` column (BIGINT) is added to the expenses table. On every update, JPA checks that the version in the database matches the version the client read. If another write has occurred in between, JPA throws `ObjectOptimisticLockingFailureException`, which is caught at the controller layer and returned as HTTP 409 Conflict. The client can then re-fetch the latest state and retry.

Pessimistic locking was considered but rejected. Concurrent expense edits are expected to be infrequent, so holding DB row locks for the duration of every request would hurt throughput in the common (no-conflict) case. JPA's `@Version` support handles the optimistic path with no additional infrastructure.

## Consequences

**Positive:**
- Concurrent conflicts are detected and surfaced rather than silently overwriting data
- No DB-level locks held during request processing
- Directly satisfies the Reliability quality attribute with testable, automated evidence
- Native JPA support -- minimal implementation effort

**Negative:**
- Clients must handle 409 responses and implement retry logic
- Under high contention, clients may receive repeated 409s before succeeding
- The version field must be included in all expense update flows

OCC is a good fit for a read-heavy, occasionally-concurrent workload like expense tracking. The client-side retry requirement is a standard pattern in collaborative editing.
