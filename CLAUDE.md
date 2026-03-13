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
