---
id: ADR-012
title: S3 and CloudFront for Static Frontend Hosting
status: Accepted
date: 2026-05-31
---

## Summary

In the context of serving a static HTML/CSS/JS frontend while routing API calls to backend services, facing the need to serve static assets securely and avoid CORS issues between the frontend and API, we decided to host the frontend in a private S3 bucket served via a CloudFront distribution with Origin Access Control, routing `/api/*` through the same distribution to the ALB, to enforce HTTPS and use a single origin for both frontend and API, accepting the added complexity of managing a CloudFront distribution.

## Context

- The frontend is a set of static HTML, CSS, and JS files (see ADR-009)
- The backend API is exposed via an ALB (see ADR-010)
- Serving frontend files from a public S3 bucket would expose the bucket directly to the internet without HTTPS enforcement
- The frontend JavaScript makes API calls that must reach the ALB -- serving both from the same CloudFront domain avoids CORS entirely

## Decision

A CloudFront distribution sits in front of both the S3 bucket and the ALB:

- Default behavior (`/*`): serves static files from the private S3 bucket via Origin Access Control. The bucket blocks all public access -- only CloudFront can read from it.
- `/api/*` cache behavior: forwards requests to the ALB with all headers and query strings, with caching disabled (TTL = 0), so API calls are always proxied through to the backend.

HTTPS is enforced globally -- HTTP requests redirect to HTTPS. Terraform generates a `deploy-config.js` file at deploy time, injecting the Cognito domain, client ID, and callback URLs so the frontend is not hardcoded to any environment.

## Consequences

**Positive:**
- Single domain for frontend and API -- no CORS configuration needed
- The S3 bucket is never directly exposed to the internet
- HTTPS enforced globally via CloudFront
- CloudFront CDN caches static assets at edge locations, reducing latency for users
- Frontend deploys are S3 object uploads -- no container rebuilds needed

**Negative:**
- CloudFront cache invalidation is needed after every frontend deploy for changes to propagate immediately
- More setup complexity compared to serving static files directly from a container
- The deploy-config.js injection via Terraform means the frontend cannot be run locally against production config without manual substitution
