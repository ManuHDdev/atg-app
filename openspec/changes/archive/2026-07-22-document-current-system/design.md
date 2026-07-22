## Context

atg-app (7 Spring Boot 4 / Java 25 microservices + Angular 20 frontend, Keycloak-secured, deployed via Docker Compose) is already implemented and running in production. This change does not introduce architecture — it records the architecture that already exists, verified against each microservice's `controller/` package, so future changes have a spec baseline to diff against.

## Goals / Non-Goals

**Goals:**
- Capture the current, verified behavior of each microservice as OpenSpec requirements.

**Non-Goals:**
- No new functionality, refactor, or migration.
- Does not resolve the known technical debt tracked in `CLAUDE.md` (zero real tests across all 7 services and the frontend, despite the "obligatorio" testing convention) — that remains tracked there, not resolved by this change.

## Decisions

No new technical decisions were made. Architecture, stack, and conventions are documented in the project's `CLAUDE.md` and are treated as the source of truth alongside the spec files created by this change.

One inconsistency was found and documented rather than "fixed": `contratos` does not split read/write access by role the way `socios`, `petroleras`, `tarjetas`, `creditos`, and `dispositivos` do — every endpoint there accepts `ADMIN`, `GESTOR`, or `USUARIO`. This is recorded as observed behavior in `specs/contratos/spec.md`, not corrected, since correcting it would be a code change outside this documentation-only change.

## Risks / Trade-offs

[Risk: baseline specs could drift from code over time] → Mitigation: future changes to these capabilities should go through the normal OpenSpec propose → spec → apply cycle.
[Risk: CI's test phase gives a false sense of safety since no tests actually run] → Mitigation: tracked as known debt in `CLAUDE.md`; add real tests before trusting the pipeline's green checkmark as a deploy gate.
