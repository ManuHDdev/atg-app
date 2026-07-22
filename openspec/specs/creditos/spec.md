# creditos Specification

## Purpose
TBD - created by archiving change document-current-system. Update Purpose after archive.
## Requirements
### Requirement: Soft delete for credit records
The `Credito` entity SHALL be soft-deleted via `activo` and `deletedAt`. A DELETE request SHALL never physically remove a row.

#### Scenario: Deleting a credito
- **WHEN** a client sends a DELETE request for a `Credito`
- **THEN** its `activo` field is set to `false` and the row remains in the database

### Requirement: Active-only listings
Listing endpoints SHALL filter results to `activo = true`.

#### Scenario: Listing excludes soft-deleted records
- **WHEN** a client lists creditos
- **THEN** only records with `activo = true` are returned

### Requirement: Dedicated database user
The `creditos` microservice SHALL connect to MySQL as `admin_creditos`, distinct from the `admin_contratos` user shared by the other microservices.

#### Scenario: Database credentials isolated per service
- **WHEN** the creditos service starts up
- **THEN** it authenticates to MySQL with its own dedicated `admin_creditos` credentials rather than the shared account

### Requirement: Read/write role split
Reading creditos SHALL require `ADMIN`, `GESTOR`, or `USUARIO`. Creating, updating, and deleting SHALL require `ADMIN` or `GESTOR` only.

#### Scenario: Usuario cannot delete a credito
- **WHEN** a user with only the `USUARIO` role sends a DELETE request
- **THEN** the backend responds with HTTP 403

