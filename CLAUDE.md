# CLAUDE.md — atg-app

## Identidad del proyecto
- Repo: ManuHDdev/atg-app
- Organización de paquetes: com.manuhd.app.[microservicio]
- Microservicios detectados: auth (8080), socios (8081), petroleras (8082), tarjetas (8083), contratos (8084), creditos (8085), dispositivos (8086)

## Stack tecnológico
- Backend: Spring Boot 4.0.0, Java 25
- Frontend: Angular 20 (standalone components)
- Base de datos: MySQL 8.0 (base de datos `atg`; creditos usa usuario `admin_creditos`, resto `admin_contratos`)
- Autenticación: Keycloak (JWT / OpenID Connect) — realm `atg`, client `atg-app`
- Contenedores: Docker + Docker Compose
- CI/CD: GitHub Actions → docker save | gzip | ssh | docker load (sin git pull en servidor)
- Documentación API: OpenAPI 3 / Swagger UI (springdoc-openapi)

## Roles del sistema (Keycloak)
- ADMIN: gestión de usuarios y configuración global
- GESTOR: acceso completo a funcionalidades de negocio
- USUARIO: acceso operativo estándar (rol por defecto)
- DEVELOPER: acceso al módulo de incidencias, resolución técnica

## Convenciones obligatorias
- Borrado lógico en todas las entidades: campos `activo` (boolean, default true) + `deletedAt` (LocalDateTime, nullable)
- DTOs separados: [NombreEntidad]RequestDTO y [NombreEntidad]ResponseDTO
- Mapeos con MapStruct (@Mapper componentModel = "spring"), nunca conversiones manuales
- Validaciones Jakarta en todos los RequestDTOs
- Swagger obligatorio en controllers: @Operation(summary) y @ApiResponse por endpoint
- Roles en controllers con @PreAuthorize("hasRole('...')")
- Listados filtran siempre por activo = true
- Tests unitarios: JUnit 5 + Mockito
- Tests de integración: Spring Boot Test + TestContainers (MySQL real, nunca H2)
- Lombok en todas las entidades y DTOs (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)

## Estructura de paquetes por microservicio
com.manuhd.app.[microservicio]
├── config/        # Seguridad, CORS, Keycloak, Swagger
├── controller/    # REST Controllers
├── dto/           # RequestDTO y ResponseDTO
├── model/         # Entidades JPA (se llama model/, no entity/)
├── enums/         # Estados, tipos, roles
├── exception/     # Excepciones personalizadas + GlobalExceptionHandler
├── mapper/        # MapStruct mappers
├── repository/    # JPA Repositories
└── service/       # Interfaces + implementaciones (impl/)

## Microservicio auth (puerto 8080)
- Contiene el módulo de incidencias bajo /api/incidencias
- Entidad Incidencia con estados: NUEVA → EN_REVISION → EN_DESARROLLO → PENDIENTE_DEPLOY → EN_PRODUCCION / DESCARTADA
- Entidad Comentario anidada en Incidencia
- Enums: EstadoIncidencia, TipoIncidencia, Prioridad
- Roles para incidencias:
  - Ver listado/detalle: ADMIN o DEVELOPER
  - Crear incidencia: cualquier usuario autenticado
  - Añadir comentario: cualquier usuario autenticado
  - Cambiar estado / notas developer: solo DEVELOPER
  - Eliminar comentario o incidencia: solo DEVELOPER

## Reglas de negocio globales
- Antes de borrar lógicamente una entidad, verificar que no tiene dependencias activas
- Excepciones personalizadas para entidad no encontrada: [Entidad]NotFoundException
- El GlobalExceptionHandler devuelve: { timestamp, status, error, message, path }
- Paginación con Pageable de Spring: ?page=0&size=20&sort=campo,asc

## Servidor de producción
- Host: 87.216.88.165, puerto SSH: 2269, usuario: manu
- Acceso: ssh -p 2269 -i ~/.ssh/scp-key manu@87.216.88.165 (SSH keys configuradas, sin contraseña)
- Compose activo: /home/manu/atg-app/docker-compose.prod.yml
- Directorio deploy: ~/atg-app
- Containers ATG en producción: auth, socios, petroleras, tarjetas, contratos, creditos, dispositivos, mysql, nginx, keycloak, mailhog, certbot

## Workflow de ramas (obligatorio)
- Cada fix o feature se desarrolla en una rama propia: `fix/descripcion` o `feat/descripcion`
- Al mergear a `main` usar siempre `--no-ff` para preservar trazabilidad de ramas
- Ejemplo: `git checkout -b fix/tipos-solicitud-orden && ... && git checkout main && git merge --no-ff fix/tipos-solicitud-orden`

## Instrucción permanente para Claude Code
ANTES de generar cualquier fichero, lee los ficheros existentes de la misma capa
para seguir exactamente el mismo patrón. Nunca asumas convenciones: verifícalas
en el código existente del microservicio correspondiente.

## Deuda técnica conocida

- **Cobertura de tests muy desigual entre módulos**: la convención de este documento (JUnit5 + Mockito, TestContainers) solo se cumple en parte. Conteos medidos ejecutando cada suite (2026-09-24), no estimados:
  - `tarjetas`: 145 tests — ciclo de solicitudes, circuito del documento firmado (adversarial), creación transaccional y GlobalExceptionHandler.
  - `dispositivos`: 103 tests — mismo circuito que `tarjetas`, con su suite adversarial. Ya no es un módulo sin tests.
  - `contratos`: ~90 métodos de test declarados (servicios, clientes REST, repositorio e integración de flujo). No se puede medir el número exacto sin Docker: su suite de integración usa TestContainers y falla en seco si el demonio no está levantado.
  - `petroleras`: 17 tests (`PlantillaDocumentoServiceTest` + `contextLoads`).
  - `creditos`: 17 tests (`CreditoServiceTest`, `ProgramadorCorreosServiceTest` + `contextLoads`).
  - `auth` y `socios`: siguen teniendo **solo** el `contextLoads` que genera Spring Initializr. Ahí `./mvnw clean verify` pasa en verde sin verificar nada; son la deuda de tests que queda por saldar en backend.
  - Frontend: 19 specs / 147 tests. La cobertura real está en `app`, `creditos`, `dispositivos`, `plantillas-documento`, `plantillas-tarjetas`, `solicitudes-tarjetas`, `zona-soltar-archivo` y los modelos; `inicio`, `login`, `registro`, `petroleras` y `socios` siguen siendo scaffolds de "should create".
  - `ng test` sí se ejecuta en CI desde 2026-08. Antes estaba deshabilitado y la suite entera estaba rota (Karma no cargaba `zone.js` y a la mayoría de specs les faltaban providers).
  - Ojo con JaCoCo: la versión fijada (0.8.12) no sabe leer class files de Java 25 (`Unsupported class file major version 69`), por eso el pipeline pasa `-Djacoco.skip=true`. Un `./mvnw verify` en local sin ese flag revienta ya en el agente durante los tests, no solo en el goal `report`.
  - Cualquier fix o feature nueva debe seguir incluyendo tests, empezando por `auth` y `socios`.

- **Documentos de socios en el historial de Git**: el commit inicial del monorepo (`5a589d2`) versionó ~190 PDFs de solicitudes reales (`SOL-2025-*`, `SOL-2026-*`) bajo `backend/contratos/storage/`, con datos personales (nombre, NIF, dirección, cuenta bancaria). El árbol de trabajo ya está limpio: `94e030f` los sacó del repo y `.gitignore` ignora `backend/*/storage/`, así que no pueden volver a colarse. **Pero siguen en el historial**: cualquier clon del repo los contiene y basta un `git show` para recuperarlos. Mientras no se purgue el historial (filter-repo + force-push coordinado, y rotación de lo que proceda), el repo sigue sin poder tratarse como público.
