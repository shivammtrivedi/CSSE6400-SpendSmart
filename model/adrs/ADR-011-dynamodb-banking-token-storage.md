---
id: ADR-011
title: DynamoDB for Banking Auth Token Storage
status: Accepted
date: 2026-05-27
---

## Summary

In the context of storing per-user OBP authentication tokens that must be retrieved quickly by user ID and encrypted at rest, facing the need to keep sensitive credentials out of the shared PostgreSQL instance, we decided to store banking tokens in a DynamoDB table encrypted with a KMS customer-managed key, to isolate sensitive credential storage from the shared application database and use a key-value access pattern that suits the data, accepting the introduction of a second database technology into the architecture.

## Context

- The banking service needs to retrieve a user's OBP credentials on every request to make live OBP API calls
- These tokens are sensitive credentials that should be encrypted at rest
- The shared PostgreSQL instance (see ADR-002) is accessible to all services -- storing credentials there increases the blast radius if any service's DB access is compromised
- The access pattern is a simple lookup: get token by userId and provider name

## Decision

OBP auth tokens are stored in a DynamoDB table (`bank-auth-tokens`) with a composite key of `userId` (partition key) and `provider` (sort key, e.g. "obp" or "bankapi"). Server-side encryption uses a KMS customer-managed key with automatic key rotation enabled.

PostgreSQL was not used for token storage for two reasons: the shared database is not an appropriate place for sensitive credentials, and the access pattern is a straightforward key-value lookup that does not benefit from a relational schema.

## Consequences

**Positive:**
- Sensitive credentials are isolated from the shared application database
- KMS CMK encryption with key rotation satisfies security requirements for credential storage
- The key-value lookup pattern is a natural fit for DynamoDB
- DynamoDB's pay-per-request billing means no cost when the banking service is idle

**Negative:**
- Introduces a second database technology -- the team needs to understand both PostgreSQL and DynamoDB
- Local development requires either a DynamoDB local emulator or live AWS credentials
- Adds another AWS-managed resource to the Terraform configuration
