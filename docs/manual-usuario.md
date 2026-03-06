# Manual de Usuario — ATG App

**Versión MVP**

---

## Índice

1. [Introducción](#1-introducción)
2. [Acceso a la aplicación](#2-acceso-a-la-aplicación)
3. [Datos maestros: configuración inicial](#3-datos-maestros-configuración-inicial)
   - 3.1 [Socios](#31-socios)
   - 3.2 [Empresas](#32-empresas)
   - 3.3 [Petroleras](#33-petroleras)
   - 3.4 [Tipos de Solicitud de Contrato](#34-tipos-de-solicitud-de-contrato)
4. [Tarjetas](#4-tarjetas)
5. [Solicitudes de Tarjetas](#5-solicitudes-de-tarjetas)
6. [Contratos](#6-contratos)
7. [Créditos](#7-créditos)
8. [Dispositivos](#8-dispositivos)
9. [Plantillas de Email](#9-plantillas-de-email)
10. [Plantillas de Tarjetas](#10-plantillas-de-tarjetas)
11. [Flujos de trabajo y estados](#11-flujos-de-trabajo-y-estados)

---

## 1. Introducción

La aplicación ATG permite gestionar de forma centralizada todos los trámites relacionados con los socios de la asociación: contratos con petroleras, solicitudes de tarjetas de combustible, créditos y dispositivos GPS.

### Orden recomendado para empezar

Antes de realizar cualquier trámite es imprescindible tener cargados los **datos maestros**. El orden correcto es:

1. Crear las **Petroleras** con las que se trabaja.
2. Definir los **Tipos de Solicitud** de cada petrolera.
3. Crear los **Socios**.
4. (Opcional) Añadir las **Empresas** vinculadas a los socios.

Una vez hecho esto ya se pueden gestionar tarjetas, contratos, créditos y dispositivos.

---

## 2. Acceso a la aplicación

[*Captura: pantalla de login*]

Para acceder introduce tu **usuario** y **contraseña** y pulsa *Iniciar sesión*. Si es la primera vez, usa el enlace *Registrarse* para crear tu cuenta.

Una vez dentro, el menú lateral izquierdo da acceso a todas las secciones de la aplicación.

---

## 3. Datos maestros: configuración inicial

### 3.1 Socios

Los socios son las personas físicas o autónomos que forman parte de la asociación. Son el eje central de la aplicación: todos los trámites (tarjetas, contratos, créditos, dispositivos) están vinculados a un socio.

#### Cómo crear un socio

1. Ir a **Socios** en el menú lateral.
2. Pulsar el botón **Nuevo socio**.
3. Rellenar el formulario:

| Campo | Obligatorio | Descripción |
|---|---|---|
| Nombre | Sí | Nombre completo del socio |
| Número de socio | Sí | Identificador único dentro de la asociación |
| Agrupación | Sí | `ATG` o `ATT` |
| Email | No | Correo electrónico de contacto |
| Teléfono | No | Número de teléfono |
| Dirección | No | Dirección postal |
| Población | No | Localidad |
| Provincia | No | Provincia |
| Código postal | No | CP |
| Es autónomo | No | Marcar si el socio actúa como autónomo |
| Activo | No | Indica si el socio está activo (por defecto: sí) |

4. Pulsar **Guardar**.

[*Captura: formulario de nuevo socio*]

#### Buscar y filtrar socios

En la lista de socios puedes buscar por **nombre**, **número de socio**, **email** o **agrupación** usando el campo de búsqueda superior.

[*Captura: listado de socios con buscador*]

#### Ficha de un socio

Al pulsar **Ver** en cualquier socio accedes a su ficha completa, organizada en tres pestañas:

- **Información general**: datos personales, dirección y empresas vinculadas.
- **Historial de solicitudes**: todas las solicitudes de contratos, tarjetas y créditos del socio, con filtros por fecha, tipo y estado.
- **Elementos activos**: contratos vigentes, tarjetas activas y créditos aprobados.

[*Captura: ficha de socio — pestaña información general*]

#### Editar o eliminar un socio

- Para **editar**: pulsa el icono de edición en la fila del socio o el botón *Editar* dentro de su ficha.
- Para **eliminar**: pulsa el icono de papelera. Se pedirá confirmación antes de borrar.

---

### 3.2 Empresas

Las empresas son entidades mercantiles vinculadas a un socio. Un mismo socio puede tener varias empresas asociadas.

#### Cómo crear una empresa

Hay dos formas:

**Desde la ficha del socio** (recomendado):
1. Abre la ficha del socio → pestaña **Información general**.
2. En la sección *Empresas*, pulsa **Añadir empresa**.
3. Rellena el formulario y guarda.

**Desde el menú Empresas**:
1. Ir a **Empresas** en el menú lateral.
2. Pulsar **Nueva empresa**.

| Campo | Obligatorio | Descripción |
|---|---|---|
| Nombre | Sí | Nombre comercial |
| Razón social | Sí | Razón social completa |
| CIF | Sí | CIF de la empresa |
| Email | Sí | Correo de contacto |
| Socio | No | Socio al que pertenece |
| Dirección | No | Dirección fiscal |
| Población | No | Localidad |
| Provincia | No | Provincia |
| Código postal | No | CP |
| Teléfono | No | Teléfono de contacto |
| Activo | Sí | Estado de la empresa |

---

### 3.3 Petroleras

Las petroleras son las compañías proveedoras de combustible con las que la asociación trabaja.

#### Cómo crear una petrolera

1. Ir a **Petroleras** en el menú lateral.
2. Pulsar **Nueva petrolera**.
3. Rellenar el formulario:

| Campo | Obligatorio | Descripción |
|---|---|---|
| Nombre | Sí | Nombre de la petrolera |
| Email | No | Email de contacto para envíos automáticos |
| Día envío créditos | No | Día del mes en que se envían los créditos |
| Activa | No | Estado (activa por defecto) |

4. Pulsar **Guardar**.

[*Captura: listado de petroleras*]

---

### 3.4 Tipos de Solicitud de Contrato

Cada petrolera tiene sus propios tipos de solicitud (por ejemplo: *Contrato de suministro*, *Baja de contrato*, *Cambio de condiciones*). Estos tipos se configuran dentro de cada petrolera y son necesarios para crear contratos.

#### Cómo añadir un tipo de solicitud

1. En la lista de **Petroleras**, pulsa **Ver** en la petrolera correspondiente.
2. Se abrirá la sección **Tipos de Solicitud** de esa petrolera.
3. Pulsa **Nuevo tipo de solicitud**.
4. Introduce el nombre, descripción y, si aplica, selecciona la plantilla PDF asociada.
5. Guarda.

[*Captura: tipos de solicitud de una petrolera*]

---

## 4. Tarjetas

Las tarjetas de combustible son el vínculo físico entre el socio y una petrolera, identificadas por la matrícula del vehículo.

#### Cómo crear una tarjeta

1. Ir a **Tarjetas** en el menú lateral.
2. Pulsar **Nueva tarjeta**.
3. Rellenar el formulario:

| Campo | Obligatorio | Descripción |
|---|---|---|
| Socio | Sí | Seleccionar el socio (campo con búsqueda por nombre, número o email) |
| Petrolera | Sí | Seleccionar la petrolera |
| Matrícula | Sí | Matrícula del vehículo |
| Número de contrato | No | Número del contrato asociado |
| Activa | No | Estado de la tarjeta |

[*Captura: formulario de nueva tarjeta*]

#### Buscar tarjetas

En la lista puedes filtrar por **socio** (buscador con autocompletado) y por **petrolera**.

[*Captura: listado de tarjetas con filtros*]

---

## 5. Solicitudes de Tarjetas

Esta sección gestiona los trámites relacionados con tarjetas: altas, bajas, duplicados y registro de llegadas.

### Panel de control (Dashboard)

[*Captura: dashboard de solicitudes de tarjetas*]

El dashboard muestra:
- **Pendientes**: solicitudes aún sin procesar.
- **Completadas hoy**: solicitudes finalizadas en el día.
- **Total completadas**: histórico total.
- **Rechazadas**: solicitudes denegadas.
- **Últimas solicitudes**: tabla con las 10 solicitudes más recientes.
- **Botones de acceso rápido**: Alta, Baja, Duplicado.

### Tipos de solicitud de tarjeta

| Tipo | Cuándo usarlo |
|---|---|
| **ALTA** | El socio solicita una nueva tarjeta |
| **BAJA** | El socio quiere dar de baja una tarjeta existente |
| **DUPLICADO** | Se solicita una tarjeta duplicada (por pérdida o deterioro) |
| **LLEGADA** | Se registra la llegada de una tarjeta y se indica la fecha estimada de entrega |

### Cómo crear una solicitud de tarjeta

1. Pulsa el botón del tipo de solicitud deseado (Alta, Baja, Duplicado) desde el dashboard, o accede a través del menú.
2. Rellena los campos del formulario:

| Campo | Obligatorio | Descripción |
|---|---|---|
| Socio | Sí | Seleccionar socio (autocompletado) |
| Petrolera | Sí | Seleccionar la petrolera |
| Matrícula | Sí | Matrícula del vehículo |
| Número de contrato | No | Si aplica |
| Tipo | Sí | Tipo de solicitud |

Para el tipo **BAJA**, además:
- Se mostrará un selector para elegir la tarjeta activa a dar de baja.

Para el tipo **LLEGADA**, además:
- Se pedirá la **fecha estimada de llegada**.

3. Pulsa **Guardar**.

[*Captura: formulario de solicitud de tarjeta*]

### Detalle de una solicitud

Al acceder al detalle de una solicitud puedes ver:
- Toda la información de la solicitud y su estado actual.
- El historial de cambios de estado.
- Los **correos enviados** relacionados con esa solicitud.
- Opciones para avanzar el estado (aprobar, rechazar, marcar como llegada, marcar como entregada).

[*Captura: detalle de solicitud de tarjeta*]

### Estados de una solicitud de tarjeta

```
PENDIENTE → APROBADA / RECHAZADA → TARJETA_LLEGADA → ENTREGADA → COMPLETADA
```

---

## 6. Contratos

Los contratos gestionan los acuerdos formales entre los socios y las petroleras. Incluyen generación de PDF, firma electrónica y seguimiento del estado.

### Cómo crear un contrato

1. Ir a **Contratos** en el menú lateral.
2. Pulsar **Nuevo contrato**.
3. Seleccionar el **tipo de solicitud**:

| Tipo | Descripción |
|---|---|
| **NUEVO** | Contrato de nueva incorporación |
| **BAJA** | Solicitud de baja de un contrato existente |
| **CAMBIO DE CONDICIONES** | Modificación de un contrato vigente |

4. Rellenar los datos del formulario:

| Campo | Obligatorio | Descripción |
|---|---|---|
| Socio | Sí | Socio titular del contrato |
| Petrolera | Sí | Petrolera a la que va dirigido |
| Tipo de contrato | Sí | Tipo de solicitud configurado en la petrolera |
| Empresa | No | Empresa del socio (si aplica) |
| Tarjeta | No | Tarjeta asociada |
| Plantilla PDF | No | Plantilla a usar para generar el documento |
| Solicitado por | No | Persona que tramita la solicitud |
| Observaciones | No | Notas internas |

5. Pulsar **Guardar**. El contrato se crea en estado **BORRADOR**.

[*Captura: formulario de nuevo contrato*]

### Flujo de trabajo de un contrato

[*Captura: diagrama de estados de un contrato o detalle con indicadores visuales*]

```
BORRADOR → ENVIADO_SOCIO → FIRMADO_SOCIO → ENVIADO_PETROLERA → ACEPTADA_PETROLERA
                                                               → RECHAZADA_PETROLERA
```

| Estado | Qué significa |
|---|---|
| **BORRADOR** | Contrato creado, pendiente de enviar al socio |
| **ENVIADO_SOCIO** | Se ha enviado el PDF al socio para su firma |
| **FIRMADO_SOCIO** | El socio ha devuelto el documento firmado |
| **ENVIADO_PETROLERA** | El contrato firmado se ha remitido a la petrolera |
| **ACEPTADA_PETROLERA** | La petrolera ha aceptado el contrato |
| **RECHAZADA_PETROLERA** | La petrolera ha rechazado el contrato |

### Gestión de PDFs

Cada contrato puede tener hasta cuatro versiones de PDF:
- **Editable**: borrador inicial generado por la aplicación.
- **Enviado**: versión enviada al socio.
- **Firmado**: documento devuelto con la firma del socio.
- **Final**: versión definitiva aceptada por la petrolera.

Desde el detalle del contrato puedes **descargar** o **visualizar** cada versión disponible.

### Listado de contratos

[*Captura: listado de contratos con filtros*]

Puedes filtrar los contratos por:
- **Petrolera**
- **Tipo de contrato**
- **Estado**

---

## 7. Créditos

La sección de créditos gestiona las solicitudes de crédito, ampliaciones y devoluciones de aval enviadas a las petroleras.

### Tipos de crédito

| Tipo | Descripción |
|---|---|
| **SOLICITUD_CREDITO** | Nueva solicitud de línea de crédito |
| **AMPLIACION_CREDITO** | Solicitud de ampliación de crédito existente |
| **DEVOLUCION_AVAL** | Solicitud de devolución de aval |

### Cómo crear una solicitud de crédito

1. Ir a **Créditos** en el menú lateral.
2. Pulsar **Nuevo crédito**.
3. Rellenar el formulario en el modal:

| Campo | Obligatorio | Descripción |
|---|---|---|
| Socio | Sí | Seleccionar socio (autocompletado) |
| Petrolera | Sí | Seleccionar la petrolera |
| Tipo de crédito | Sí | Ver tabla de tipos anterior |
| Empresa | No | Empresa del socio (si aplica) |
| Monto | No | Importe solicitado (según tipo) |
| Observaciones | No | Notas o comentarios adicionales |

4. Pulsar **Guardar**.

[*Captura: formulario de nuevo crédito*]

### Envío a la petrolera

Una vez creado el crédito en estado **PENDIENTE**, puedes:
- **Enviar ahora**: envía la solicitud inmediatamente a la petrolera.
- **Programar envío**: selecciona una fecha y hora para que el envío se realice automáticamente.

### Estados de un crédito

```
PENDIENTE → ENVIADO_PETROLERA → APROBADO → COMPLETADO_APROBADO
                              → DENEGADO → COMPLETADO_DENEGADO
```

| Estado | Qué significa |
|---|---|
| **PENDIENTE** | Crédito creado, pendiente de enviar |
| **ENVIADO_PETROLERA** | Solicitud enviada, esperando respuesta |
| **APROBADO** | La petrolera ha aprobado el crédito |
| **DENEGADO** | La petrolera ha denegado el crédito |
| **COMPLETADO_APROBADO** | Trámite finalizado con resultado positivo |
| **COMPLETADO_DENEGADO** | Trámite finalizado con resultado negativo |

### Historial de correos

Desde el detalle de cada crédito puedes consultar el **log de correos** enviados: fecha, destinatario y asunto de cada comunicación automática generada.

[*Captura: log de correos de un crédito*]

---

## 8. Dispositivos

La sección de dispositivos gestiona los equipos GPS u otros dispositivos vinculados a los vehículos de los socios.

### Tipos de solicitud de dispositivo

| Tipo | Descripción |
|---|---|
| **ALTA_DISPOSITIVO** | Solicitud de instalación de nuevo dispositivo |
| **SOLICITUD_CREDITO** | Solicitud de crédito asociada a un dispositivo |
| **BAJA_DISPOSITIVO** | Solicitud de retirada de un dispositivo |
| **CAMBIO_MATRICULA** | Cambio de matrícula en un dispositivo existente |

### Cómo crear una solicitud de dispositivo

1. Ir a **Dispositivos** en el menú lateral.
2. Pulsar **Nueva solicitud**.
3. Rellenar el formulario:

| Campo | Obligatorio | Descripción |
|---|---|---|
| Socio | Sí | Seleccionar socio (autocompletado) |
| Petrolera | Sí | Seleccionar la petrolera |
| Tipo de solicitud | Sí | Ver tabla de tipos anterior |
| Matrícula | Sí | Matrícula del vehículo |
| Empresa | No | Empresa del socio (si aplica) |
| Dispositivo | No | Para tipos BAJA y CAMBIO_MATRICULA: seleccionar dispositivo activo |
| Matrícula destino | Solo CAMBIO_MATRICULA | Nueva matrícula a asignar |
| Monto | Solo SOLICITUD_CREDITO | Importe solicitado |
| Observaciones | No | Notas adicionales |

Al seleccionar un socio, la aplicación carga automáticamente sus empresas y dispositivos activos.

4. Pulsar **Guardar**.

[*Captura: formulario de nueva solicitud de dispositivo*]

### Envío a la petrolera y estados

Al igual que en créditos, puedes **enviar ahora** o **programar el envío**.

```
PENDIENTE → ENVIADO_PETROLERA → APROBADO → COMPLETADO
                              → DENEGADO → COMPLETADO
```

### Filtros disponibles

En el listado de dispositivos puedes filtrar por **estado** y por **tipo de solicitud**.

---

## 9. Plantillas de Email

Las plantillas de email definen los mensajes automáticos que la aplicación envía en cada evento del flujo de trabajo (creación, envío, respuesta de la petrolera, etc.).

### Cómo crear una plantilla

1. Ir a **Plantillas Email** en el menú lateral.
2. Pulsar **Nueva plantilla**.
3. Rellenar el formulario:

| Campo | Obligatorio | Descripción |
|---|---|---|
| Nombre | Sí | Nombre identificativo de la plantilla |
| Asunto | Sí | Asunto del correo electrónico |
| Cuerpo | Sí | Contenido del email (admite HTML) |
| Tipo de evento | Sí | Evento que dispara este email (ver tabla) |
| Petrolera | No | Si aplica solo a una petrolera concreta |
| Activa | Sí | Si la plantilla está en uso |

### Variables disponibles

Puedes usar variables en el asunto y cuerpo del email que se sustituyen automáticamente al enviar:

| Variable | Valor |
|---|---|
| `{nombre}` | Nombre del socio |
| `{nif}` | NIF del socio |
| `{email}` | Email del socio |
| `{telefono}` | Teléfono del socio |
| `{provincia}` | Provincia del socio |
| `{matricula}` | Matrícula del vehículo |
| `{numeroContrato}` | Número de contrato |
| `{fecha}` | Fecha actual |
| `{nombrePetrolera}` | Nombre de la petrolera |
| `{emailPetrolera}` | Email de la petrolera |

### Tipos de evento

| Evento | Cuándo se dispara |
|---|---|
| `SOLICITUD_CREDITO` | Nueva solicitud de crédito creada |
| `AMPLIACION_CREDITO` | Solicitud de ampliación de crédito |
| `DEVOLUCION_AVAL` | Solicitud de devolución de aval |
| `ALTA_DISPOSITIVO` | Solicitud de alta de dispositivo |
| `BAJA_DISPOSITIVO` | Solicitud de baja de dispositivo |
| `CAMBIO_MATRICULA` | Solicitud de cambio de matrícula |
| `CONTRATO_PETROLERA` | Envío de contrato a petrolera |
| `NOTIF_SOCIO_CREADO` | Notificación al socio al crear un trámite |
| `NOTIF_SOCIO_ENVIADO` | Notificación al socio cuando se envía a petrolera |
| `NOTIF_SOCIO_RESULTADO` | Notificación al socio con el resultado final |

---

## 10. Plantillas de Tarjetas

Las plantillas de tarjetas definen los emails específicos para los trámites de tarjetas de combustible.

### Tipos de plantilla de tarjeta

| Tipo | Descripción |
|---|---|
| `LLEGADA_MADRID` | Notificación de llegada para socios de Madrid |
| `LLEGADA_FUERA` | Notificación de llegada para socios fuera de Madrid |
| `ALTA_SOCIO` | Comunicación al socio del alta de tarjeta |
| `ALTA_PETROLERA` | Comunicación a la petrolera del alta |
| `BAJA_SOCIO` | Comunicación al socio de la baja de tarjeta |
| `DUPLICADO_SOCIO` | Comunicación al socio del duplicado |

### Cómo editar una plantilla de tarjeta

1. Ir a **Plantillas Tarjetas** en el menú lateral.
2. Pulsa **Editar** en la plantilla que quieras modificar.
3. Modifica el asunto o el cuerpo usando las mismas variables disponibles indicadas en el apartado anterior.
4. Guarda los cambios.

---

## 11. Flujos de trabajo y estados

### Resumen de estados por módulo

#### Contratos
| Estado | Color indicativo |
|---|---|
| BORRADOR | Gris |
| ENVIADO_SOCIO | Azul |
| FIRMADO_SOCIO | Amarillo |
| ENVIADO_PETROLERA | Naranja |
| ACEPTADA_PETROLERA | Verde |
| RECHAZADA_PETROLERA | Rojo |

#### Créditos
| Estado | Color indicativo |
|---|---|
| PENDIENTE | Gris |
| ENVIADO_PETROLERA | Azul |
| APROBADO | Verde claro |
| DENEGADO | Rojo claro |
| COMPLETADO_APROBADO | Verde |
| COMPLETADO_DENEGADO | Rojo |

#### Dispositivos
| Estado | Color indicativo |
|---|---|
| PENDIENTE | Gris |
| ENVIADO_PETROLERA | Azul |
| APROBADO | Verde claro |
| DENEGADO | Rojo claro |
| COMPLETADO | Verde |

#### Solicitudes de tarjetas
| Estado | Color indicativo |
|---|---|
| PENDIENTE | Gris |
| APROBADA | Verde claro |
| RECHAZADA | Rojo |
| TARJETA_LLEGADA | Azul |
| ENTREGADA | Amarillo |
| COMPLETADA | Verde |

---

### Envío programado

Los módulos de **Créditos** y **Dispositivos** permiten programar el envío a la petrolera para una fecha y hora futuras. Esto es útil cuando los envíos deben realizarse en un día concreto del mes (por ejemplo, el día configurado en la petrolera para el envío de créditos).

Para programar un envío:
1. Abre el trámite que está en estado **PENDIENTE**.
2. Selecciona la opción **Programar envío**.
3. Elige la **fecha** y la **hora**.
4. Confirma. El sistema enviará automáticamente en el momento indicado.

---

*Fin del manual*
