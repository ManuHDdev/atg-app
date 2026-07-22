## ADDED Requirements

### Requirement: Soft delete for member contracts
The `ContratoSocio` entity SHALL be soft-deleted via `activo` and `deletedAt`. A DELETE request SHALL never physically remove a row.

#### Scenario: Deleting a contrato
- **WHEN** a client sends a DELETE request for a `ContratoSocio`
- **THEN** its `activo` field is set to `false` and the row remains in the database

### Requirement: Active-only listings
Listing endpoints SHALL filter results to `activo = true`.

#### Scenario: Listing excludes soft-deleted records
- **WHEN** a client lists contratos
- **THEN** only records with `activo = true` are returned

### Requirement: Uniform role access
Every endpoint in this microservice (read and write) SHALL require at least `ADMIN`, `GESTOR`, or `USUARIO` — there is no elevated-only mutation split observed in this service, unlike socios/petroleras/tarjetas/creditos.

#### Scenario: Usuario can create a contrato
- **WHEN** a user with only the `USUARIO` role sends a POST request to create a `ContratoSocio`
- **THEN** the backend accepts the request
