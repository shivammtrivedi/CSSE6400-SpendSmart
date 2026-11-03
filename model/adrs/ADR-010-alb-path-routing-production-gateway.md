---
id: ADR-010
title: AWS ALB Path-Based Routing as Production API Gateway
status: Accepted
date: 2026-05-24
---

## Summary

In the context of routing API requests to multiple backend services in production, facing the operational overhead of running a dedicated Spring Cloud Gateway container in ECS, we decided to use AWS Application Load Balancer path-based listener rules as the API gateway in production, to reduce the number of running containers and simplify routing configuration, accepting that dev and production routing behaviour differ and that there is no request-level middleware at the gateway layer.

## Context

- The local development setup used a Spring Cloud Gateway service and Eureka service registry for routing and service discovery
- In production, services are deployed as ECS Fargate tasks reachable via AWS Cloud Map private DNS (`service-name.spendsmart.local`)
- Running a dedicated gateway container in ECS adds cost and a failure point without providing anything the ALB cannot do for path-based routing
- The ALB is already required to expose services to the CloudFront distribution (see ADR-012)

## Decision

The AWS ALB handles all inbound API routing in production via path-based listener rules:

- `/api/v1/expense*` -- expense-service
- `/api/v1/banking*` -- banking-service
- `/api/v1/query*` -- query-service
- `/api/v1/alerts*` -- alerts-service
- `/api/v1/budgets*` -- budget-service
- `/api/*` -- bank-api-nodejs (catch-all, evaluated last)

The Spring Cloud Gateway service and Eureka service registry are not deployed to production. JWT validation that was handled by the gateway in local dev is handled by each service individually in production via the Cognito JWKS endpoint.

## Consequences

**Positive:**
- No dedicated gateway container to run, monitor, or scale
- Listener rules are declarative Terraform and easy to audit
- ALB health checks handle service availability monitoring without additional tooling
- Integrates directly with CloudFront (see ADR-012)

**Negative:**
- Dev and production routing behaviour differ -- Spring Gateway is used locally, ALB is used in production
- No gateway-level middleware for cross-cutting concerns like rate limiting, circuit breaking, or request tracing
- Adding a new service requires a Terraform change and redeploy to register new listener rules
- JWT validation is the responsibility of each individual service rather than a single enforcement point
