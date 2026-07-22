# auth Specification

## Purpose
TBD - created by archiving change document-current-system. Update Purpose after archive.
## Requirements
### Requirement: JWT authentication via Keycloak
Every microservice SHALL validate requests as an OAuth2 resource server against the `atg` Keycloak realm. No microservice SHALL implement its own login flow.

#### Scenario: Unauthenticated request rejected
- **WHEN** a client calls a protected endpoint without a valid JWT
- **THEN** the backend responds with HTTP 401

### Requirement: Four-tier role model
The system SHALL support four realm roles: `ADMIN` (user management and global config), `GESTOR` (full business access), `USUARIO` (standard operational access, default role), `DEVELOPER` (incident-tracking module access).

#### Scenario: Admin-only user management
- **WHEN** a user without the `ADMIN` role calls a user-management endpoint
- **THEN** the backend responds with HTTP 403

### Requirement: Incident tracking (incidencias) with fixed lifecycle
The `Incidencia` entity SHALL move through the states `NUEVA → EN_REVISION → EN_DESARROLLO → PENDIENTE_DEPLOY → EN_PRODUCCION`, or be marked `DESCARTADA`. Only the `DEVELOPER` role SHALL change state, add developer notes, or delete an incidencia/comentario.

#### Scenario: Any authenticated user can report an incident
- **WHEN** any authenticated user submits a new incidencia
- **THEN** it is created in state `NUEVA`

#### Scenario: Non-developer cannot change state
- **WHEN** a user without the `DEVELOPER` role attempts to change an incidencia's state
- **THEN** the backend responds with HTTP 403

### Requirement: Viewing incidencias restricted to ADMIN/DEVELOPER
Listing and viewing incidencia detail SHALL require the `ADMIN` or `DEVELOPER` role — `GESTOR` and `USUARIO` SHALL NOT see the incident list even though they can create one.

#### Scenario: Gestor cannot list incidencias
- **WHEN** a user with only the `GESTOR` role calls the incidencias list endpoint
- **THEN** the backend responds with HTTP 403

