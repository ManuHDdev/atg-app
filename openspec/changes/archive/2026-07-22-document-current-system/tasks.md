## 1. Baseline documentation

- [x] 1.1 Verify auth behavior against source code (Keycloak JWT, four-tier roles, incidencias lifecycle and role gating)
- [x] 1.2 Verify socios behavior against source code (soft delete, active-only listings, read/write role split)
- [x] 1.3 Verify petroleras behavior against source code (soft delete, active-only listings, read/write role split)
- [x] 1.4 Verify tarjetas behavior against source code (soft delete, request workflow, read/write role split)
- [x] 1.5 Verify contratos behavior against source code (soft delete, active-only listings, uniform role access — no elevated-only split)
- [x] 1.6 Verify creditos behavior against source code (soft delete, dedicated DB user, read/write role split)
- [x] 1.7 Verify dispositivos behavior against source code (soft delete, request workflow, read/write role split)
- [x] 1.8 Write delta specs for all 7 capabilities under `specs/`

No implementation work is required — this change is documentation-only and reflects behavior already implemented and deployed in production.
