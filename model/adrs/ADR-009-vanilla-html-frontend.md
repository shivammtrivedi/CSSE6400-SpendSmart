---
id: ADR-009
title: Vanilla HTML/CSS/JS Frontend, No JavaScript Framework
status: Accepted
date: 2026-05-20
---

## Summary

In the context of building a multi-page web application frontend for SpendSmart, facing the additional build tooling and learning overhead of a JavaScript framework, we decided to write the frontend as plain HTML pages with vanilla CSS and JavaScript, to eliminate build steps and allow any team member to work on any page without framework knowledge, accepting limitations in component reuse and client-side state management.

## Context

- The frontend has roughly a dozen distinct pages (expenses, groups, dashboard, budgets, alerts, bank connect, etc.)
- The team's primary expertise is in Java/Spring Boot -- frontend experience varies across members
- The deployment target is S3, which hosts static files directly (see ADR-012)
- No team member had prior experience shipping a shared React or Vue codebase in a team setting

## Decision

The frontend is implemented as a collection of standalone `.html` files with shared CSS and JavaScript files included via `<script>` and `<link>` tags. No bundler (Webpack, Vite), no framework (React, Vue, Angular), and no package.json for frontend dependencies. JavaScript is written directly against the browser DOM API.

## Consequences

**Positive:**
- No build step required -- files can be edited and deployed directly to S3
- Any team member can open and modify an HTML file without learning a framework
- No frontend dependency management
- Works naturally with the S3 + CloudFront hosting setup (see ADR-012)

**Negative:**
- No component system -- shared UI elements like the navbar must be duplicated across pages or injected via JavaScript
- Client-side state has to be managed manually with localStorage or in-memory variables
- More verbose code for common patterns (fetch wrappers, form handling, conditional rendering)
- Harder to scale the frontend if the feature set grows significantly
