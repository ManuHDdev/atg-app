-- Script para verificar los contratos activos en la base de datos

USE atg;

-- Ver todas las columnas de contratos_socio
SELECT 'Estructura de contratos_socio:' AS info;
DESCRIBE contratos_socio;

-- Ver todos los contratos
SELECT 'Todos los contratos:' AS info;
SELECT id, socio_id, petrolera_id, activo, fecha_vigencia_desde, fecha_vigencia_hasta, tipo_contrato, estado
FROM contratos_socio;

-- Ver contratos activos
SELECT 'Contratos activos:' AS info;
SELECT id, socio_id, petrolera_id, activo, fecha_vigencia_desde, tipo_contrato, estado
FROM contratos_socio
WHERE activo = TRUE;

-- Ver contratos por socio
SELECT 'Contratos por socio:' AS info;
SELECT socio_id, COUNT(*) as total, SUM(activo) as activos
FROM contratos_socio
GROUP BY socio_id;

-- Ver migraciones aplicadas
SELECT 'Migraciones Flyway aplicadas:' AS info;
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;
