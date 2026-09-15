# Suite de Tests - Microservicio Contratos

Este documento describe la suite completa de tests unitarios y de integración creada para el microservicio de contratos.

## 📋 Tabla de Contenidos

- [Configuración](#configuración)
- [Tests Unitarios](#tests-unitarios)
- [Tests de Integración](#tests-de-integración)
- [Cobertura de Tests](#cobertura-de-tests)
- [Ejecución de Tests](#ejecución-de-tests)
- [Estructura de Archivos](#estructura-de-archivos)

---

## ⚙️ Configuración

### Archivos de Configuración

#### `application-test.properties`
Configuración específica para el entorno de testing:
- Base de datos H2 en memoria (modo MySQL)
- Directorios de almacenamiento temporal para tests
- Email deshabilitado (modo simulación)
- Logging en modo DEBUG
- JPA en modo `create-drop` para reiniciar BD en cada test

#### `TestBase.java`
Clase base abstracta con utilidades comunes para todos los tests:
- Builders de entidades de prueba (TipoContrato, PlantillaContrato, SolicitudContrato, etc.)
- Constantes de IDs de prueba
- Métodos helper para crear MockMultipartFile (PDFs simulados)
- Generadores de datos de prueba

### Dependencias Agregadas al POM

```xml
<!-- H2 Database para testing -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>

<!-- AssertJ para mejores assertions -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 🧪 Tests Unitarios

### 1. TipoContratoServiceTest
**Ubicación**: `service/TipoContratoServiceTest.java`

**Tests implementados** (23 tests):
- ✅ `listarTodos()` - Retorna todos los tipos, lista vacía
- ✅ `listarActivos()` - Solo tipos activos
- ✅ `obtenerPorId()` - Por ID existente e inexistente
- ✅ `obtenerPorCodigo()` - Por código existente e inexistente
- ✅ `crear()` - Creación exitosa y con código duplicado
- ✅ `actualizar()` - Actualización exitosa, cambio de código, código duplicado, tipo inexistente
- ✅ `eliminar()` - Eliminación exitosa y tipo inexistente
- ✅ `activar()` - Activación exitosa y tipo inexistente
- ✅ `desactivar()` - Desactivación exitosa y tipo inexistente

**Técnicas utilizadas**:
- Mocking con `@Mock` y `@InjectMocks`
- `ArgumentCaptor` para verificar argumentos pasados a mocks
- AssertJ para assertions fluidas
- Verificación de interacciones con `verify()`

---

### 2. CreditoServiceTest
**Ubicación**: `service/CreditoServiceTest.java`

**Tests implementados** (17 tests):
- ✅ `listarTodos()` - Lista completa de créditos
- ✅ `obtenerPorId()` - Por ID existente e inexistente
- ✅ `listarPorSocio()` - Créditos de un socio específico
- ✅ `listarPorPetrolera()` - Créditos de una petrolera
- ✅ `listarPorEstado()` - Filtrado por estado
- ✅ `crear()` - Envío inmediato y programado
- ✅ `enviarAPetrolera()` - Envío exitoso, validación de estado, email fallback
- ✅ `responderPetrolera()` - Aprobación y denegación con notificación automática
- ✅ `notificarSocio()` - Notificación exitosa y validación de estado
- ✅ Manejo de errores en servicios externos

**Técnicas utilizadas**:
- Mock de RestTemplate para llamadas a microservicios
- Mock de EmailService para envío de correos
- Simulación de respuestas de APIs externas
- Verificación de transiciones de estado

---

### 3. EmailServiceTest
**Ubicación**: `service/EmailServiceTest.java`

**Tests implementados** (8 tests):
- ✅ `enviarCorreoSimple()` - Envío habilitado/deshabilitado, múltiples destinatarios
- ✅ `enviarCorreoHTML()` - Envío HTML habilitado/deshabilitado
- ✅ Manejo de excepciones al enviar
- ✅ Verificación de from, to, subject, content

**Técnicas utilizadas**:
- `ReflectionTestUtils` para setear campos privados (@Value)
- `ArgumentCaptor` para capturar mensajes enviados
- Mock de `JavaMailSender` y `MimeMessage`

---

### 4. PetrolerasClientTest
**Ubicación**: `client/PetrolerasClientTest.java`

**Tests implementados** (12 tests):
- ✅ `obtenerPlantillaPdf()` - Descarga exitosa, errores HTTP, RestTemplate exceptions
- ✅ `tienePlantillaPdf()` - Con plantilla, sin plantilla, ruta vacía, con excepción
- ✅ `obtenerPetrolera()` - Obtención exitosa, errores HTTP, exceptions, body null

**Técnicas utilizadas**:
- Mock de RestTemplate con diferentes ResponseEntity
- Simulación de errores de red (RestClientException)
- Verificación de URLs llamadas

---

### 5. SociosClientTest
**Ubicación**: `client/SociosClientTest.java`

**Tests implementados** (6 tests):
- ✅ `obtenerSocio()` - Obtención exitosa con todos los campos, socio inactivo
- ✅ Manejo de errores HTTP 404
- ✅ Manejo de excepciones de RestTemplate
- ✅ Body null

**Técnicas utilizadas**:
- Mock de RestTemplate
- Construcción de DTOs completos para verificación
- Verificación de URLs correctas

---

### 6. TipoContratoControllerTest
**Ubicación**: `controller/TipoContratoControllerTest.java`

**Tests implementados** (11 tests):
- ✅ `GET /api/tipos-contrato` - Listar todos
- ✅ `GET /api/tipos-contrato/activos` - Listar activos
- ✅ `GET /api/tipos-contrato/{id}` - Obtener por ID (200, 404)
- ✅ `GET /api/tipos-contrato/codigo/{codigo}` - Por código
- ✅ `POST /api/tipos-contrato` - Crear (201, error código duplicado)
- ✅ `PUT /api/tipos-contrato/{id}` - Actualizar
- ✅ `DELETE /api/tipos-contrato/{id}` - Eliminar
- ✅ `POST /api/tipos-contrato/{id}/activar` - Activar
- ✅ `POST /api/tipos-contrato/{id}/desactivar` - Desactivar

**Técnicas utilizadas**:
- `@WebMvcTest` para tests de controllers aislados
- `MockMvc` para simular peticiones HTTP
- `@MockBean` para mockear servicios
- Verificación de JSON responses con `jsonPath()`
- Verificación de status codes HTTP

---

## 🔗 Tests de Integración

### 1. TipoContratoRepositoryIntegrationTest
**Ubicación**: `repository/TipoContratoRepositoryIntegrationTest.java`

**Tests implementados** (10 tests):
- ✅ `findAll()` - Todos los tipos
- ✅ `findByActivoTrue()` - Solo activos
- ✅ `findByCodigo()` - Búsqueda exitosa y vacía
- ✅ `existsByCodigo()` - True/False
- ✅ `save()` - Crear nuevo y actualizar existente
- ✅ `deleteById()` - Eliminación
- ✅ Constraint UNIQUE en código
- ✅ `findById()` - Búsqueda por ID

**Técnicas utilizadas**:
- `@DataJpaTest` para tests de repositorio con BD real
- `TestEntityManager` para manipular BD directamente
- H2 en memoria para aislamiento
- `@Transactional` para rollback automático

---

### 2. SolicitudContratoFlowIntegrationTest
**Ubicación**: `integration/SolicitudContratoFlowIntegrationTest.java`

**Tests implementados** (5 tests):
- ✅ Crear solicitud en estado BORRADOR
- ✅ Transición BORRADOR → ENVIADO_SOCIO
- ✅ Validación de transiciones inválidas (máquina de estados)
- ✅ Generación de números de solicitud únicos y secuenciales
- ✅ Búsqueda por número de solicitud

**Técnicas utilizadas**:
- `@SpringBootTest` para contexto completo de Spring
- `@MockBean` para mockear clientes externos (PetrolerasClient)
- `@Transactional` para rollback
- Tests del flujo completo end-to-end

**Flujo de estados testeado**:
```
BORRADOR → ENVIADO_SOCIO → FIRMADO_SOCIO → ENVIADO_PETROLERA
```

---

## 📊 Cobertura de Tests

### Resumen de Cobertura

| Componente | Tests Unitarios | Tests Integración | Total Tests |
|------------|----------------|-------------------|-------------|
| **Services** | ✅ 48 tests | - | 48 |
| **Clients** | ✅ 18 tests | - | 18 |
| **Controllers** | ✅ 11 tests | - | 11 |
| **Repositories** | - | ✅ 10 tests | 10 |
| **Flujos Integración** | - | ✅ 5 tests | 5 |
| **TOTAL** | **77** | **15** | **92 tests** |

### Servicios Cubiertos

| Servicio | Estado | # Tests |
|----------|--------|---------|
| TipoContratoService | ✅ Completo | 23 |
| CreditoService | ✅ Completo | 17 |
| EmailService | ✅ Completo | 8 |
| SolicitudContratoService | ⚠️ Parcial | 5 (integración) |
| ContratoSocioService | ⚠️ Pendiente | 0 |
| PlantillaContratoService | ⚠️ Pendiente | 0 |
| PdfService | ⚠️ Pendiente | 0 |

### Controllers Cubiertos

| Controller | Estado | # Tests |
|------------|--------|---------|
| TipoContratoController | ✅ Completo | 11 |
| CreditoController | ⚠️ Pendiente | 0 |
| SolicitudContratoController | ⚠️ Pendiente | 0 |
| ContratoSocioController | ⚠️ Pendiente | 0 |
| PlantillaContratoController | ⚠️ Pendiente | 0 |

### Clients Cubiertos

| Client | Estado | # Tests |
|--------|--------|---------|
| PetrolerasClient | ✅ Completo | 12 |
| SociosClient | ✅ Completo | 6 |

### Repositories Cubiertos

| Repository | Estado | # Tests |
|------------|--------|---------|
| TipoContratoRepository | ✅ Completo | 10 |
| SolicitudContratoRepository | ⚠️ Parcial | 5 (en flujo) |
| CreditoRepository | ⚠️ Pendiente | 0 |
| ContratoSocioRepository | ⚠️ Pendiente | 0 |
| PlantillaContratoRepository | ⚠️ Pendiente | 0 |

---

## 🚀 Ejecución de Tests

### Ejecutar Todos los Tests

```bash
# Desde la raíz del proyecto contratos
mvn clean test
```

### Ejecutar Tests Específicos

```bash
# Solo tests unitarios
mvn test -Dtest="*Test"

# Solo tests de integración
mvn test -Dtest="*IntegrationTest"

# Un test específico
mvn test -Dtest=TipoContratoServiceTest
mvn test -Dtest=CreditoServiceTest

# Tests de un paquete
mvn test -Dtest="com.manuhd.app.contratos.service.*Test"
```

### Ejecutar con Cobertura (JaCoCo)

```bash
mvn clean test jacoco:report
# Ver reporte en: target/site/jacoco/index.html
```

### Ejecutar desde el IDE

**IntelliJ IDEA**:
1. Clic derecho en el paquete `src/test/java` → "Run 'All Tests'"
2. O clic derecho en un test específico → "Run '[TestName]'"
3. Atajos: `Ctrl+Shift+F10` (Run), `Ctrl+Shift+F9` (Debug)

**Eclipse**:
1. Clic derecho en el proyecto → "Run As" → "JUnit Test"
2. O clic derecho en un test → "Run As" → "JUnit Test"

---

## 📁 Estructura de Archivos

```
src/test/
├── java/com/manuhd/app/contratos/
│   ├── TestBase.java                           # Clase base con utilidades
│   │
│   ├── service/                                # Tests de servicios
│   │   ├── TipoContratoServiceTest.java       # ✅ 23 tests
│   │   ├── CreditoServiceTest.java            # ✅ 17 tests
│   │   └── EmailServiceTest.java              # ✅ 8 tests
│   │
│   ├── client/                                 # Tests de clientes
│   │   ├── PetrolerasClientTest.java          # ✅ 12 tests
│   │   └── SociosClientTest.java              # ✅ 6 tests
│   │
│   ├── controller/                             # Tests de controllers
│   │   └── TipoContratoControllerTest.java    # ✅ 11 tests
│   │
│   ├── repository/                             # Tests de repositorios
│   │   └── TipoContratoRepositoryIntegrationTest.java  # ✅ 10 tests
│   │
│   └── integration/                            # Tests de integración
│       └── SolicitudContratoFlowIntegrationTest.java   # ✅ 5 tests
│
└── resources/
    └── application-test.properties             # Configuración de testing
```

---

## 🎯 Casos de Uso Cubiertos

### Gestión de Tipos de Contrato
- ✅ CRUD completo
- ✅ Activación/desactivación
- ✅ Validación de códigos únicos
- ✅ Filtrado por estado activo

### Gestión de Créditos
- ✅ Creación de solicitudes de crédito
- ✅ Envío a petrolera (inmediato y programado)
- ✅ Respuesta de petrolera (aprobado/denegado)
- ✅ Notificación a socios
- ✅ Transiciones de estado (PENDIENTE → ENVIADO → APROBADO/DENEGADO → COMPLETADO)
- ✅ Integración con servicios externos (Socios, Petroleras)
- ✅ Envío de emails HTML

### Gestión de Solicitudes de Contrato
- ✅ Creación de solicitudes con generación automática de número
- ✅ Máquina de estados (BORRADOR → ENVIADO_SOCIO → FIRMADO_SOCIO → ENVIADO_PETROLERA)
- ✅ Validación de transiciones de estado
- ✅ Integración con microservicio Petroleras para obtener PDFs

### Comunicación con Microservicios
- ✅ Cliente Petroleras: Obtener plantillas PDF, verificar plantillas, obtener info petrolera
- ✅ Cliente Socios: Obtener información de socios
- ✅ Manejo de errores de red y timeouts
- ✅ Fallbacks cuando servicios no responden

### Envío de Emails
- ✅ Emails simples y HTML
- ✅ Modo simulación para testing
- ✅ Configuración dinámica (habilitado/deshabilitado)
- ✅ Múltiples destinatarios

---

## ✨ Mejores Prácticas Implementadas

1. **Aislamiento de Tests**
   - Cada test es independiente
   - `@Transactional` para rollback automático
   - Base de datos H2 en memoria

2. **Nomenclatura Clara**
   - Patrón: `metodoATestear_DebeHacerX_CuandoY()`
   - `@DisplayName` descriptivos en español

3. **AAA Pattern**
   - Given (Arrange): Preparar datos
   - When (Act): Ejecutar acción
   - Then (Assert): Verificar resultado

4. **Cobertura de Casos**
   - ✅ Happy path (camino feliz)
   - ✅ Edge cases (casos extremos)
   - ✅ Error handling (manejo de errores)
   - ✅ Null safety
   - ✅ Validaciones de negocio

5. **Mocking Apropiado**
   - Solo mockear dependencias externas
   - No mockear clases bajo test
   - Usar `verify()` para verificar interacciones

6. **Assertions Expresivas**
   - AssertJ para assertions fluidas y legibles
   - Mensajes de error descriptivos

---

## 📝 Tests Pendientes (Recomendados)

### Alta Prioridad
- [ ] **SolicitudContratoService** - Tests unitarios completos
- [ ] **PdfService** - Procesamiento de PDFs (crítico)
- [ ] **ContratoSocioService** - CRUD y lógica de baja
- [ ] **PlantillaContratoService** - Gestión de plantillas
- [ ] **SolicitudContratoController** - API REST completa
- [ ] **CreditoController** - API REST de créditos

### Media Prioridad
- [ ] **GlobalExceptionHandler** - Manejo global de errores
- [ ] **SolicitudContratoRepository** - Queries personalizadas
- [ ] **CreditoRepository** - Filtros y queries complejas
- [ ] **Flujo de Créditos** - Test de integración end-to-end
- [ ] **ContratoSocioController** - API REST

### Baja Prioridad
- [ ] **TipoSolicitudPetroleraService** - Gestión de tipos
- [ ] **WebConfig** - Configuración CORS
- [ ] **AppConfig** - Configuración de beans

---

## 🔍 Debugging de Tests

### Ver Logs Detallados

Agregar a `application-test.properties`:
```properties
logging.level.org.springframework=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.com.manuhd.app.contratos=TRACE
```

### Verificar Base de Datos H2

Agregar a `application-test.properties`:
```properties
spring.h2.console.enabled=true
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
```

### Deshabilitar Rollback (para debugging)

En un test específico:
```java
@Test
@Rollback(false)  // Mantiene los datos después del test
void miTest() {
    // ...
}
```

---

## 📚 Recursos y Referencias

### Documentación
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)

### Anotaciones Principales
- `@SpringBootTest` - Carga contexto completo
- `@DataJpaTest` - Tests de repositorio
- `@WebMvcTest` - Tests de controllers
- `@Mock` - Mock de Mockito
- `@MockBean` - Mock de Spring
- `@InjectMocks` - Inyectar mocks

---

## 👥 Contribuir

Para agregar nuevos tests:

1. Extender `TestBase` si necesitas utilidades comunes
2. Usar el perfil `@ActiveProfiles("test")`
3. Seguir la convención de nombres: `[Clase]Test` o `[Clase]IntegrationTest`
4. Agregar `@DisplayName` descriptivos
5. Documentar casos complejos con comentarios
6. Actualizar este README con la cobertura agregada

---

**Última actualización**: 2026-02-03
**Tests totales**: 92 (77 unitarios + 15 integración)
**Cobertura estimada**: ~45% del código crítico
