# Microservicio de Créditos - ATG

## Descripción

Microservicio independiente para la gestión de créditos, solicitudes y devoluciones de aval. Se comunica con los microservicios de Socios y Petroleras para obtener información necesaria.

## Puerto

**8085**

## Funcionalidades

- ✅ Gestión de créditos (Solicitud, Ampliación, Devolución de Aval)
- ✅ Envío automático de correos a petroleras
- ✅ Notificaciones automáticas a socios
- ✅ Programación de envíos por fecha o día de la semana
- ✅ Control de estados y respuestas de petroleras
- ✅ Plantillas de correo personalizables
- ✅ Modo simulación para pruebas

## Configuración

### Base de Datos

Usuario: `admin_creditos`
Password: `CreditosDev2024!`
Base de datos: `atg`

Ejecutar script SQL para crear usuario:
```bash
mysql -u root -p < d:/ATG/app/backend/sql/crear_usuario_creditos.sql
```

Ejecutar script para crear tablas:
```bash
mysql -u admin_creditos -p atg < d:/ATG/app/backend/sql/creditos_y_plantillas_seguro.sql
```

### Configuración SMTP

Por defecto en modo simulación (`app.email.enabled=false`).

Para habilitar envío real de correos, editar `application.properties`:
```properties
app.email.enabled=true
spring.mail.username=tu-email@gmail.com
spring.mail.password=tu-password
```

## Endpoints

### Créditos

- `GET /api/creditos` - Listar todos
- `GET /api/creditos/{id}` - Obtener por ID
- `GET /api/creditos/socio/{socioId}` - Listar por socio
- `GET /api/creditos/petrolera/{petroleraId}` - Listar por petrolera
- `GET /api/creditos/estado/{estado}` - Listar por estado
- `POST /api/creditos` - Crear crédito
- `POST /api/creditos/{id}/enviar-petrolera` - Enviar a petrolera
- `POST /api/creditos/{id}/responder` - Responder (petrolera)
- `POST /api/creditos/{id}/notificar-socio` - Notificar al socio

## Dependencias

Comunicación con:
- **Socios** (puerto 8081) - Obtener datos de socios y empresas
- **Petroleras** (puerto 8082) - Obtener datos de petroleras y plantillas de correo

## Ejecutar

```bash
cd d:\ATG\app\backend\creditos
mvn spring-boot:run
```

O con Maven wrapper:
```bash
./mvnw spring-boot:run
```

## Estados del Crédito

- `PENDIENTE` - Creado, esperando envío
- `ENVIADO_PETROLERA` - Enviado a la petrolera
- `APROBADO` - Aprobado por la petrolera
- `DENEGADO` - Denegado por la petrolera
- `COMPLETADO` - Proceso completado (socio notificado)

## Tipos de Crédito

- `SOLICITUD_CREDITO` - Nueva solicitud de crédito
- `AMPLIACION_CREDITO` - Ampliación de crédito existente
- `DEVOLUCION_AVAL` - Devolución de aval del socio

## Programación Automática

El servicio `ProgramadorCorreosService` ejecuta tareas automáticas:

- **Diaria a las 9:00 AM** - Procesa créditos programados para la fecha actual
- **Jueves a las 9:00 AM** - Procesa envíos específicos para petroleras que requieren día específico

## Logs

Los correos en modo simulación se registran en los logs con formato:
```
Envío de correo SIMULADO (email.enabled=false)
Destinatario: email@ejemplo.com
Asunto: Asunto del correo
Cuerpo: Contenido...
```
