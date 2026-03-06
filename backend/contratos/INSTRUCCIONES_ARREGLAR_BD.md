# 🔧 Instrucciones para Arreglar la Base de Datos

## 🚀 Solución Rápida (Recomendada)

Ejecuta el script automático que arregla todo:

### Windows:
1. Abre una terminal en: `d:\ATG\app\backend\contratos\`
2. Ejecuta: `arreglar_bd.bat`
3. Ingresa la contraseña de MySQL cuando te la pida

### Linux/Mac o Manual:
```bash
cd d:\ATG\app\backend\contratos
mysql -u root -p atg < arreglo_completo.sql
```

---

## ❓ ¿Qué hace este script?

1. ✅ Hace nullable las columnas `tipo_contrato_id` y `plantilla_id`
2. ✅ Inserta datos de prueba de contratos activos para los socios 4 y 8
3. ✅ Muestra un resumen de verificación

---

## ⚠️ IMPORTANTE

**Si ya tienes contratos reales en tu base de datos:**
1. Abre el archivo `arreglo_completo.sql`
2. Comenta o elimina la **PARTE 2** (datos de prueba)
3. Luego ejecuta el script

---

## 📋 Problemas que resuelve

### Problema 1: Error "Column 'plantilla_id' cannot be null"
**Causa:** Las migraciones V5 y V6 de Flyway no se aplicaron.
**Solución:** El script ejecuta manualmente los ALTER TABLE necesarios.

### Problema 2: "No hay contratos activos" al intentar BAJA
**Causa:** La tabla `contratos_socio` está vacía o no tiene contratos activos.
**Solución:** El script inserta 4 contratos de prueba activos.

---

## ✅ Verificación después de ejecutar

Después de ejecutar el script, deberías ver:

1. **Columnas modificadas:**
   - `tipo_contrato_id`: IS_NULLABLE = YES
   - `plantilla_id`: IS_NULLABLE = YES

2. **Contratos activos insertados:**
   - Socio 4 → Petroleras 1 y 2
   - Socio 8 → Petroleras 1 y 4

3. **Resumen de contratos por socio**

---

## 🔄 Pasos siguientes

1. **Reinicia el backend de contratos**
   ```bash
   # En la carpeta del backend de contratos
   mvn spring-boot:run
   ```

2. **Prueba crear un nuevo contrato**
   - Ya NO debería dar error de `plantilla_id`

3. **Prueba BAJA de contrato**
   - Selecciona el socio 4 u 8
   - Deberías ver solo las petroleras con las que tienen contratos activos
   - Al seleccionar petrolera, debería cargar los contratos disponibles

---

## 🐛 Si sigue sin funcionar

### Verificar manualmente la base de datos:

```sql
USE atg;

-- Ver columnas
DESCRIBE solicitudes_contrato;

-- Ver contratos activos
SELECT * FROM contratos_socio WHERE activo = TRUE;

-- Ver migraciones Flyway aplicadas
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;
```

### Si Flyway marcó las migraciones como fallidas:

```sql
-- Limpiar entradas de Flyway para V5 y V6
DELETE FROM flyway_schema_history WHERE version IN ('5', '6');
```

Luego reinicia el backend para que Flyway las intente ejecutar de nuevo.

---

## 📝 Archivos creados

- `arreglo_completo.sql` - Script unificado que arregla todo
- `arreglar_bd.bat` - Script automático para Windows
- `fix_nullable_columns.sql` - Solo arregla columnas (sin datos de prueba)
- `verificar_contratos.sql` - Script de verificación
- `INSTRUCCIONES_ARREGLAR_BD.md` - Este archivo
