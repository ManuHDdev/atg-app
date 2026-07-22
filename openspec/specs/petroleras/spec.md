# petroleras Specification

## Purpose
TBD - created by archiving change document-current-system. Update Purpose after archive.
## Requirements
### Requirement: Soft delete for fuel-company records
The `Petrolera` entity SHALL be soft-deleted via `activo` and `deletedAt`. A DELETE request SHALL never physically remove a row.

#### Scenario: Deleting a petrolera
- **WHEN** a client sends a DELETE request for a `Petrolera`
- **THEN** its `activo` field is set to `false` and the row remains in the database

### Requirement: Active-only listings
Listing endpoints SHALL filter results to `activo = true`.

#### Scenario: Listing excludes soft-deleted records
- **WHEN** a client lists petroleras
- **THEN** only records with `activo = true` are returned

### Requirement: Read/write role split
Reading petroleras SHALL require `ADMIN`, `GESTOR`, or `USUARIO`. Creating, updating, and deleting SHALL require `ADMIN` or `GESTOR` only.

#### Scenario: Usuario cannot create
- **WHEN** a user with only the `USUARIO` role sends a POST request to create a petrolera
- **THEN** the backend responds with HTTP 403

