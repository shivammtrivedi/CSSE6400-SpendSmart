---
id: ADR-003
title: AWS Cognito for Authentication
status: Accepted
date: 2026-05-19
---

## Summary

In the context of replacing a custom JWT implementation that stored user credentials in PostgreSQL, facing the security risk of managing password hashing and token signing ourselves, we decided to delegate authentication entirely to AWS Cognito User Pools, to remove credential storage from the application database and hand token lifecycle management to AWS, accepting a string userId type change across all services and a hard dependency on AWS in the auth path.

## Context

The initial auth service implemented custom JWT generation using JJWT, with users stored in a PostgreSQL table. This required managing password hashing, token signing secrets, token expiry, and a users table.

## Decision

Replace the custom JWT implementation with AWS Cognito User Pools. The auth service becomes a thin wrapper over the Cognito SDK, exposing the same REST API (`POST /api/auth/register`, `POST /api/auth/login`) but delegating all credential storage, password hashing, and token issuance to Cognito. Rolling our own JWT signing and password hashing introduces unnecessary risk -- any mistake in the implementation could expose user credentials, and Cognito eliminates that class of problem entirely.

The gateway validates incoming JWTs against Cognito's JWKS endpoint using Spring Security's OAuth2 resource server, extracting the `sub` claim as the user identifier forwarded via `X-User-Id`.

## Consequences

**Positive:**
- No user credential storage in the application database -- Cognito manages this securely
- JWT validation uses AWS-maintained public key infrastructure
- Built-in support for password policies, account lockout, and MFA if needed later
- Removes the users table from the shared database entirely

**Negative:**
- Cognito user IDs are UUIDs (strings), not numeric longs -- all services must use `String` for userId instead of `Long`, affecting every entity and controller
- Local development requires AWS credentials or a Cognito emulator (Cognito has no official local emulator)
- Adds AWS vendor dependency to the authentication path

The security improvement and removal of custom credential management outweigh the development overhead of the string userId migration and the AWS dependency.
