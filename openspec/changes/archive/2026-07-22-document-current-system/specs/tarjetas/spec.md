## ADDED Requirements

### Requirement: Soft delete for cards, templates, and requests
`Tarjeta`, `PlantillaTarjeta`, and `SolicitudTarjeta` entities SHALL be soft-deleted via `activo` and `deletedAt`. A DELETE request SHALL never physically remove a row.

#### Scenario: Deleting a tarjeta
- **WHEN** a client sends a DELETE request for a `Tarjeta`
- **THEN** its `activo` field is set to `false` and the row remains in the database

### Requirement: Card request workflow separate from card management
`SolicitudTarjeta` (card requests) SHALL be managed through its own controller, independent of direct card CRUD, allowing a request/approval flow before a `Tarjeta` exists.

#### Scenario: Creating a card request
- **WHEN** an authorized user submits a new `SolicitudTarjeta`
- **THEN** it is created without directly creating a `Tarjeta`

### Requirement: Read/write role split
Reading tarjetas, plantillas, and solicitudes SHALL require `ADMIN`, `GESTOR`, or `USUARIO`. Creating, updating, and deleting SHALL require `ADMIN` or `GESTOR` only.

#### Scenario: Usuario cannot delete a plantilla
- **WHEN** a user with only the `USUARIO` role sends a DELETE request for a `PlantillaTarjeta`
- **THEN** the backend responds with HTTP 403
