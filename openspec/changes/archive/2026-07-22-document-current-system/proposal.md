## Why

atg-app (7 Spring Boot microservices + Angular frontend, fronted by Keycloak) was built before adopting spec-driven development. There is no OpenSpec baseline describing what the system currently does. This change establishes that baseline from the verified, already-implemented code — it does not introduce new functionality.

## What Changes

- Create baseline specs for the 7 microservice capabilities, reflecting current, already-implemented and already-deployed behavior verified against each service's `controller/` package.
- No code changes beyond this audit's housekeeping (removing a stray `dispositivos2.zip` and documenting the testing gap in `CLAUDE.md`, done separately from this change).

## Capabilities

### New Capabilities
- `auth`: Keycloak-backed authentication, user administration, and the incidencias (issue tracking) module.
- `socios`: Members and their companies (Empresa/Socio).
- `petroleras`: Fuel-company records.
- `tarjetas`: Fuel cards, card templates, and card requests.
- `contratos`: Member contracts.
- `creditos`: Credit records.
- `dispositivos`: Devices and device requests.

### Modified Capabilities
(none — this is the first baseline)

## Impact

Documentation only: creates `openspec/specs/{auth,socios,petroleras,tarjetas,contratos,creditos,dispositivos}/spec.md`. No source code, infrastructure, or deployed behavior is affected.
