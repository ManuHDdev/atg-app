# Instrucciones para Configurar Datos de Prueba

## Problema
Para probar la funcionalidad de **BAJA de contratos**, necesitas tener registros en la tabla `contratos_socio` con `activo = TRUE`.

**Nota importante**: Las **solicitudes de contrato** (`solicitudes_contrato`) NO son lo mismo que los **contratos finales** (`contratos_socio`). Para hacer BAJA, necesitas contratos en la tabla `contratos_socio`.

## Opciones para Insertar Datos de Prueba

### Opción 1: Migración Automática (Recomendado)

La migración `V4__insert_contratos_prueba.sql` se ejecutará automáticamente al reiniciar el backend.

**Pasos:**
1. Verifica que tienes socios y petroleras en tu base de datos
2. Ajusta los IDs en el archivo `V4__insert_contratos_prueba.sql` si es necesario:
   ```sql
   -- Cambia estos valores según tu BD:
   socio_id: 1, 2, 3
   petrolera_id: 1, 2
   ```
3. Reinicia el microservicio de contratos
4. Flyway ejecutará automáticamente la migración V4

**Verificar que funcionó:**
```sql
SELECT id, socio_id, petrolera_id, tipo_contrato, activo, fecha_vigencia_desde
FROM contratos_socio
WHERE activo = TRUE;
```

### Opción 2: Inserción Manual

Si prefieres control total, usa el archivo `datos_prueba_contratos.sql`:

**Pasos:**
1. Abre tu cliente de base de datos (MySQL Workbench, DBeaver, etc.)
2. Ejecuta las consultas de verificación para ver IDs disponibles:
   ```sql
   SELECT id, nombre, nif FROM socios WHERE activo = true;
   SELECT id, nombre FROM petroleras WHERE activa = true;
   ```
3. Ajusta los valores de `socio_id` y `petrolera_id` en los INSERT del archivo
4. Ejecuta los INSERT manualmente

## Verificación de Contratos Activos

### Ver todos los contratos activos:
```sql
SELECT
    id,
    socio_id,
    petrolera_id,
    tipo_contrato,
    activo,
    fecha_vigencia_desde,
    estado
FROM contratos_socio
WHERE activo = TRUE;
```

### Ver contratos de un socio específico con una petrolera:
```sql
SELECT
    id,
    tipo_contrato,
    activo,
    fecha_vigencia_desde
FROM contratos_socio
WHERE socio_id = 1
  AND petrolera_id = 1
  AND activo = TRUE;
```

### Ver todos los contratos (activos e inactivos):
```sql
SELECT
    id,
    socio_id,
    petrolera_id,
    tipo_contrato,
    activo,
    fecha_vigencia_desde,
    fecha_vigencia_hasta,
    estado
FROM contratos_socio
ORDER BY activo DESC, fecha_vigencia_desde DESC;
```

## Probar la Funcionalidad de BAJA

Una vez que tengas contratos activos:

1. **Frontend**: Accede al formulario de contratos
2. Selecciona **"Baja de Contrato"** en Tipo de Solicitud
3. Busca y selecciona un socio usando el autocomplete
4. Selecciona una petrolera
5. **Deberías ver**: El dropdown de contratos se llena con los contratos activos
6. Selecciona un contrato y envía el formulario

**Resultado esperado:**
- El contrato se marca como `activo = FALSE`
- Se registra `fecha_vigencia_hasta = HOY`
- Se envía un email a la petrolera (si `app.email.enabled=true`)
- Se ve el email simulado en los logs (si `app.email.enabled=false`)

## Troubleshooting

### "No hay contratos activos para este socio y petrolera"

**Causas posibles:**
1. No existen contratos en la tabla `contratos_socio`
2. Todos los contratos están con `activo = FALSE`
3. Los IDs de socio o petrolera no coinciden
4. Hay un error en el endpoint del backend

**Solución:**
```sql
-- Verificar si HAY contratos:
SELECT COUNT(*) FROM contratos_socio;

-- Ver qué socios/petroleras tienen contratos activos:
SELECT
    socio_id,
    petrolera_id,
    COUNT(*) as cantidad
FROM contratos_socio
WHERE activo = TRUE
GROUP BY socio_id, petrolera_id;
```

### Error al ejecutar la migración V4

Si la migración falla porque los IDs de socio/petrolera no existen:

1. **Opción A**: Comenta o elimina temporalmente `V4__insert_contratos_prueba.sql`
2. **Opción B**: Usa la Opción 2 (inserción manual) con IDs correctos
3. **Opción C**: Primero inserta socios/petroleras de prueba

## Estructura de la Tabla contratos_socio

```sql
CREATE TABLE contratos_socio (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    socio_id BIGINT NOT NULL,
    empresa_id BIGINT,
    tarjeta_id BIGINT,
    plantilla_id BIGINT,
    petrolera_id BIGINT,
    subseccion_petrolera_id BIGINT,
    tipo_contrato VARCHAR(255),
    tipo_solicitante VARCHAR(50),
    fecha_hora_solicitud DATETIME,
    solicitado_por VARCHAR(100),
    estado VARCHAR(50),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_vigencia_desde DATE,
    fecha_vigencia_hasta DATE,
    ruta_borrador VARCHAR(500),
    ruta_enviado VARCHAR(500),
    ruta_firmado VARCHAR(500),
    ruta_final VARCHAR(500),
    fecha_envio_socio DATETIME,
    fecha_recepcion_firmado DATETIME,
    fecha_envio_petrolera DATETIME,
    observaciones TEXT,
    created_at DATETIME,
    updated_at DATETIME
);
```

## Contacto

Si tienes problemas con los datos de prueba, revisa los logs del backend para ver más detalles de los errores.
