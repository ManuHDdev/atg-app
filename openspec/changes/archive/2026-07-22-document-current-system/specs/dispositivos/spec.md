## ADDED Requirements

### Requirement: Soft delete for devices and device requests
`Dispositivo` and `SolicitudDispositivo` entities SHALL be soft-deleted via `activo` and `deletedAt`. A DELETE request SHALL never physically remove a row.

#### Scenario: Deleting a dispositivo
- **WHEN** a client sends a DELETE request for a `Dispositivo`
- **THEN** its `activo` field is set to `false` and the row remains in the database

### Requirement: Device request workflow separate from device management
`SolicitudDispositivo` (device requests) SHALL be managed through its own controller, independent of direct device CRUD, allowing a request/approval flow before a `Dispositivo` exists.

#### Scenario: Creating a device request
- **WHEN** an authorized user submits a new `SolicitudDispositivo`
- **THEN** it is created without directly creating a `Dispositivo`

### Requirement: Read/write role split
Reading dispositivos and solicitudes SHALL require `ADMIN`, `GESTOR`, or `USUARIO`. Creating, updating, and deleting a `SolicitudDispositivo` SHALL require `ADMIN` or `GESTOR` only. `DispositivoController` in the current implementation exposes only read-oriented endpoints, all under the shared 3-role check.

#### Scenario: Usuario cannot approve a device request
- **WHEN** a user with only the `USUARIO` role attempts to update a `SolicitudDispositivo`'s status
- **THEN** the backend responds with HTTP 403
