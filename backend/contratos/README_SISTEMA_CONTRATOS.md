# Sistema de Gestión de Solicitudes de Contratos

## Descripción General

Sistema completo y flexible para la gestión de solicitudes de contratos con PDFs editables. Permite crear, gestionar y procesar solicitudes de contratos con diferentes petroleras, utilizando plantillas PDF dinámicas y flujo de estados.

## Arquitectura del Sistema

### Modelo de Datos

#### 1. TipoContrato
Define los tipos de contrato disponibles:
- **ALTA**: Nueva solicitud de contrato
- **BAJA**: Cancelación de contrato existente
- **CAMBIO_CONDICIONES**: Modificación de contrato existente

#### 2. TipoSolicitudPetrolera
Define las subsecciones dinámicas por petrolera. Ejemplos:
- MOEVE + ALTA → "Precio Lista", "Precio Poste", "Eurotrafic", "Via T"
- SOLRED + ALTA → "Contrato Poste", "Contrato Profesional", etc.
- Cualquier Petrolera + BAJA → null (mismo PDF para todas)

#### 3. PlantillaContrato (Actualizada)
Almacena las plantillas PDF editables con relaciones a:
- `petroleraId`: ID de la petrolera
- `tipoContratoId`: ID del tipo de contrato
- `tipoSolicitudPetroleraId`: ID del subtipo (nullable para plantillas genéricas)

#### 4. SolicitudContrato (Nueva Entidad Principal)
Gestiona todo el ciclo de vida de una solicitud:
- Información del socio/empresa/tarjeta
- Referencias a petrolera y tipo de contrato
- Estado del workflow
- Rutas de los diferentes PDFs (editable, enviado, firmado, final)
- Fechas de seguimiento

### Estados de Solicitud

```
BORRADOR → ENVIADO_SOCIO → FIRMADO_SOCIO → ENVIADO_PETROLERA
```

**Flujo de trabajo:**
1. **BORRADOR**: Solicitud creada, PDF editable disponible para modificación
2. **ENVIADO_SOCIO**: PDF aplanado y enviado al socio para firma
3. **FIRMADO_SOCIO**: PDF firmado recibido del socio
4. **ENVIADO_PETROLERA**: PDF final enviado a la petrolera

## Estructura de Almacenamiento

```
storage/
├── plantillas/
│   └── originales/
│       ├── petrolera_1/
│       │   ├── tipo_1/
│       │   │   ├── subtipo_1/
│       │   │   │   └── 1234567890_plantilla.pdf
│       │   │   ├── subtipo_2/
│       │   │   └── plantilla_generica.pdf
│       │   ├── tipo_2/
│       │   └── tipo_3/
│       ├── petrolera_2/
│       └── petrolera_3/
└── contratos/
    ├── solicitudes/
    │   ├── SOL-2024-00001/
    │   │   ├── editable.pdf
    │   │   ├── enviado.pdf
    │   │   ├── firmado.pdf
    │   │   └── final.pdf
    │   └── SOL-2024-00002/
    └── borradores/
```

## API REST Endpoints

### TipoContratoController (`/api/tipos-contrato`)

- `GET /` - Listar todos los tipos de contrato
- `GET /activos` - Listar tipos activos
- `GET /{id}` - Obtener por ID
- `GET /codigo/{codigo}` - Obtener por código (ALTA, BAJA, etc.)
- `POST /` - Crear nuevo tipo
- `PUT /{id}` - Actualizar tipo
- `DELETE /{id}` - Eliminar tipo
- `PATCH /{id}/activar` - Activar tipo
- `PATCH /{id}/desactivar` - Desactivar tipo

### TipoSolicitudPetroleraController (`/api/tipos-solicitud-petrolera`)

- `GET /` - Listar todos
- `GET /{id}` - Obtener por ID
- `GET /petrolera/{petroleraId}` - Listar por petrolera
- `GET /petrolera/{petroleraId}/tipo/{tipoContratoId}` - Listar por petrolera y tipo
- `GET /petrolera/{petroleraId}/tipo/{tipoContratoId}/activas` - Listar activas
- `POST /` - Crear nuevo tipo
- `PUT /{id}` - Actualizar tipo
- `DELETE /{id}` - Eliminar tipo
- `PATCH /{id}/activar` - Activar
- `PATCH /{id}/desactivar` - Desactivar

### PlantillaContratoController (`/api/plantillas`)

- `GET /` - Listar todas las plantillas
- `GET /activas` - Listar plantillas activas
- `GET /{id}` - Obtener por ID
- `GET /petrolera/{petroleraId}` - Listar por petrolera
- `GET /petrolera/{petroleraId}/activas` - Listar activas por petrolera
- `GET /petrolera/{petroleraId}/tipo/{tipoContratoId}` - Listar por petrolera y tipo
- `GET /criterios?petroleraId=&tipoContratoId=&tipoSolicitudPetroleraId=` - Buscar plantilla por criterios
- `GET /{id}/campos` - Obtener campos del PDF
- `POST /` - Subir nueva plantilla (multipart/form-data)
- `PUT /{id}` - Actualizar plantilla
- `PUT /{id}/archivo` - Actualizar archivo PDF
- `DELETE /{id}` - Eliminar (desactivar) plantilla
- `PATCH /{id}/activar` - Activar plantilla
- `PATCH /{id}/desactivar` - Desactivar plantilla

### SolicitudContratoController (`/api/solicitudes`)

#### Crear y Listar
- `POST /` - Crear nueva solicitud
- `GET /` - Listar solicitudes con filtros y paginación
  - Parámetros: `socioId`, `petroleraId`, `tipoContratoId`, `estado`, `fechaDesde`, `fechaHasta`, `page`, `size`, `sortBy`, `sortDirection`
- `GET /{id}` - Obtener solicitud por ID
- `GET /numero/{numeroSolicitud}` - Obtener por número de solicitud

#### Gestión de PDFs
- `GET /{id}/pdf/editable` - Descargar PDF editable
- `POST /{id}/pdf/editable` - Guardar cambios en PDF editable (multipart)
- `GET /{id}/pdf/{tipo}` - Descargar cualquier PDF (editable, enviado, firmado, final)

#### Workflow
- `POST /{id}/enviar-socio` - Aplanar PDF y enviar a socio (BORRADOR → ENVIADO_SOCIO)
- `POST /{id}/pdf/firmado` - Subir PDF firmado (ENVIADO_SOCIO → FIRMADO_SOCIO)
- `POST /{id}/enviar-petrolera` - Enviar a petrolera (FIRMADO_SOCIO → ENVIADO_PETROLERA)
- `PUT /{id}/estado` - Cambiar estado manualmente

## Ejemplos de Uso

### 1. Crear una Solicitud

```bash
POST /api/solicitudes
Content-Type: application/json

{
  "socioId": 1,
  "empresaId": 5,
  "tarjetaId": null,
  "petroleraId": 1,
  "tipoContratoId": 1,
  "tipoSolicitudPetroleraId": 2,
  "solicitadoPor": "Juan Pérez",
  "esAutonomo": false,
  "observaciones": "Solicitud urgente"
}
```

**Respuesta:**
```json
{
  "id": 1,
  "numeroSolicitud": "SOL-2024-00001",
  "socioId": 1,
  "empresaId": 5,
  "petroleraId": 1,
  "tipoContratoId": 1,
  "tipoSolicitudPetroleraId": 2,
  "plantillaId": 3,
  "estado": "BORRADOR",
  "rutaPdfEditable": "./storage/contratos/solicitudes/SOL-2024-00001/editable.pdf",
  "fechaHoraSolicitud": "2024-01-15T10:30:00",
  "fechaCreacion": "2024-01-15T10:30:00"
}
```

### 2. Workflow Completo

```bash
# 1. Descargar PDF editable para edición
GET /api/solicitudes/1/pdf/editable

# 2. (Usuario edita el PDF externamente)

# 3. Guardar PDF editado
POST /api/solicitudes/1/pdf/editable
Content-Type: multipart/form-data
file: [archivo_editado.pdf]

# 4. Enviar a socio (aplana el PDF)
POST /api/solicitudes/1/enviar-socio

# 5. Subir PDF firmado por el socio
POST /api/solicitudes/1/pdf/firmado
Content-Type: multipart/form-data
file: [archivo_firmado.pdf]

# 6. Enviar a petrolera
POST /api/solicitudes/1/enviar-petrolera
```

### 3. Listar Solicitudes con Filtros

```bash
GET /api/solicitudes?socioId=1&estado=ENVIADO_SOCIO&page=0&size=20&sortBy=fechaCreacion&sortDirection=DESC
```

### 4. Subir Plantilla de Contrato

```bash
POST /api/plantillas
Content-Type: multipart/form-data

plantilla: {
  "nombrePlantilla": "MOEVE Alta Precio Lista",
  "descripcion": "Plantilla para alta de contrato MOEVE con precio lista",
  "petroleraId": 1,
  "tipoContratoId": 1,
  "tipoSolicitudPetroleraId": 1,
  "activa": true
}
archivo: [plantilla.pdf]
```

## Servicios Principales

### PdfService
Gestiona todas las operaciones con PDFs:
- `guardarPlantillaOrganizada()` - Guarda plantilla en estructura organizada
- `copiarPlantillaParaSolicitud()` - Copia plantilla para nueva solicitud
- `aplanarPdfParaSolicitud()` - Aplana PDF (elimina campos editables)
- `guardarPdfFirmado()` - Guarda PDF firmado
- `copiarPdfFinal()` - Copia PDF final para envío
- `leerPdf()` - Lee PDF como bytes para descarga
- `validarPdfEditable()` - Valida que PDF tenga campos editables
- `extraerCamposPdf()` - Extrae nombres de campos del PDF

### SolicitudContratoService
Servicio principal del sistema:
- `crearSolicitud()` - Crea solicitud y copia plantilla
- `abrirPdfEditable()` - Obtiene PDF editable
- `guardarPdfEditado()` - Guarda cambios en PDF
- `enviarASocio()` - Aplana y cambia estado
- `subirPdfFirmado()` - Recibe PDF firmado
- `enviarAPetrolera()` - Finaliza proceso
- `descargarPdf()` - Descarga cualquier tipo de PDF
- `listarSolicitudes()` - Lista con filtros y paginación
- `generarNumeroSolicitud()` - Genera número secuencial

### PlantillaContratoService
- `obtenerPlantillaPorCriterios()` - Busca plantilla específica o genérica

## Validaciones Importantes

### Cambios de Estado
- Solo se permiten transiciones válidas:
  - BORRADOR → ENVIADO_SOCIO
  - ENVIADO_SOCIO → FIRMADO_SOCIO
  - FIRMADO_SOCIO → ENVIADO_PETROLERA
  - ENVIADO_PETROLERA (estado final)

### Plantillas
- Al subir plantilla, se valida que el PDF sea editable (tenga AcroForm)
- Se extraen automáticamente los campos del PDF
- Se almacenan en estructura organizada por petrolera/tipo/subtipo

### Solicitudes
- Se valida que exista plantilla para los criterios especificados
- El número de solicitud es único y secuencial por año
- Solo se pueden editar PDFs en estado BORRADOR
- Solo se pueden subir PDFs firmados en estado ENVIADO_SOCIO

## Datos Iniciales

El script SQL `V2__datos_iniciales_contratos.sql` incluye:
- 3 tipos de contrato básicos (ALTA, BAJA, CAMBIO_CONDICIONES)
- Tipos de solicitud para 4 petroleras principales (MOEVE, SOLRED, GALP, REPSOL)
- Consultas de ejemplo útiles

## Configuración

En `application.properties`:
```properties
# Almacenamiento
storage.plantillas=./storage/plantillas/originales
storage.contratos=./storage/contratos/solicitudes
storage.borradores=./storage/contratos/borradores

# Archivos grandes (50MB)
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

## Próximos Pasos / TODOs

1. **Envío de Emails**
   - Implementar envío de email al socio cuando se envía solicitud
   - Implementar envío de email a petrolera cuando se finaliza proceso

2. **Notificaciones**
   - Sistema de notificaciones en tiempo real para cambios de estado

3. **Auditoría**
   - Registro completo de todas las acciones sobre una solicitud
   - Historial de cambios de estado

4. **Validaciones Adicionales**
   - Validar que socio, empresa, tarjeta existan en sus respectivos microservicios
   - Validar que petrolera exista

5. **Firma Digital**
   - Integración con servicios de firma digital
   - Validación de firmas digitales

6. **Búsqueda Avanzada**
   - Búsqueda por texto completo en observaciones
   - Filtros adicionales

## Tecnologías Utilizadas

- **Spring Boot 3.x**
- **Apache PDFBox** - Manipulación de PDFs
- **JPA/Hibernate** - ORM
- **MySQL** - Base de datos
- **Lombok** - Reducción de código boilerplate
- **Jakarta Validation** - Validaciones

## Estructura del Proyecto

```
backend/contratos/
├── src/main/java/com/manuhd/app/contratos/
│   ├── controller/
│   │   ├── TipoContratoController.java
│   │   ├── TipoSolicitudPetroleraController.java
│   │   ├── PlantillaContratoController.java
│   │   └── SolicitudContratoController.java
│   ├── dto/
│   │   ├── TipoContratoDTO.java
│   │   ├── TipoSolicitudPetroleraDTO.java
│   │   ├── CrearSolicitudDTO.java
│   │   ├── SolicitudContratoDTO.java
│   │   ├── FiltroSolicitudesDTO.java
│   │   └── SubirPlantillaDTO.java
│   ├── model/
│   │   ├── TipoContrato.java
│   │   ├── TipoSolicitudPetrolera.java
│   │   ├── PlantillaContrato.java
│   │   ├── SolicitudContrato.java
│   │   └── EstadoSolicitud.java (enum)
│   ├── repository/
│   │   ├── TipoContratoRepository.java
│   │   ├── TipoSolicitudPetroleraRepository.java
│   │   ├── PlantillaContratoRepository.java
│   │   └── SolicitudContratoRepository.java
│   └── service/
│       ├── TipoContratoService.java
│       ├── TipoSolicitudPetroleraService.java
│       ├── PlantillaContratoService.java
│       ├── SolicitudContratoService.java
│       └── PdfService.java
└── src/main/resources/
    ├── application.properties
    └── db/migration/
        └── V2__datos_iniciales_contratos.sql
```

## Contacto y Soporte

Para preguntas o problemas, contactar al equipo de desarrollo.
