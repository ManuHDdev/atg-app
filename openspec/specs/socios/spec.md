# socios Specification

## Purpose
TBD - created by archiving change document-current-system. Update Purpose after archive.
## Requirements
### Requirement: Soft delete for members and companies
`Socio` and `Empresa` entities SHALL be soft-deleted via `activo` (boolean, default true) and `deletedAt` (nullable timestamp). A DELETE request SHALL never physically remove a row.

#### Scenario: Deleting a socio
- **WHEN** a client sends a DELETE request for a `Socio`
- **THEN** its `activo` field is set to `false` and the row remains in the database

### Requirement: Active-only listings
Listing endpoints for `Socio` and `Empresa` SHALL filter results to `activo = true`.

#### Scenario: Listing excludes soft-deleted records
- **WHEN** a client lists socios
- **THEN** only records with `activo = true` are returned

### Requirement: Read/write role split
Reading socios/empresas SHALL require `ADMIN`, `GESTOR`, or `USUARIO`. Creating, updating, and deleting SHALL require `ADMIN` or `GESTOR` only.

#### Scenario: Usuario cannot delete
- **WHEN** a user with only the `USUARIO` role sends a DELETE request
- **THEN** the backend responds with HTTP 403

