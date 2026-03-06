-- ========================================
-- SCRIPT COMPLETO DE ARREGLO - ATG CONTRATOS
-- ========================================
-- Este script arregla los problemas de:
-- 1. Columnas que no permiten NULL
-- 2. Datos de prueba faltantes

USE atg;

-- ========================================
-- PARTE 1: ARREGLAR COLUMNAS
-- ========================================

-- Hacer tipo_contrato_id nullable
ALTER TABLE solicitudes_contrato
MODIFY COLUMN tipo_contrato_id BIGINT NULL COMMENT 'ID del tipo de contrato (opcional)';

-- Hacer plantilla_id nullable
ALTER TABLE solicitudes_contrato
MODIFY COLUMN plantilla_id BIGINT NULL COMMENT 'ID de la plantilla (opcional)';

SELECT 'Columnas modificadas correctamente' AS resultado;

-- ========================================
-- PARTE 2: INSERTAR DATOS DE PRUEBA
-- ========================================

-- Verificar si ya existen datos
SELECT COUNT(*) as contratos_existentes FROM contratos_socio;

-- Insertar solo si no hay datos (comentar si ya existen)
-- Si ya tienes datos, OMITE ESTA PARTE

-- Contrato 1: Socio 4 con Petrolera 1
INSERT IGNORE INTO contratos_socio (
    socio_id,
    petrolera_id,
    activo,
    fecha_vigencia_desde,
    tipo_contrato,
    estado,
    tipo_solicitante,
    observaciones,
    created_at,
    updated_at
) VALUES (
    4, 1, TRUE, CURDATE() - INTERVAL 6 MONTH,
    'Contrato General', 'COMPLETADO', 'AUTONOMO',
    'Contrato de prueba', NOW(), NOW()
);

-- Contrato 2: Socio 4 con Petrolera 2
INSERT IGNORE INTO contratos_socio (
    socio_id, petrolera_id, activo, fecha_vigencia_desde,
    tipo_contrato, estado, tipo_solicitante,
    observaciones, created_at, updated_at
) VALUES (
    4, 2, TRUE, CURDATE() - INTERVAL 3 MONTH,
    'Contrato Poste', 'COMPLETADO', 'AUTONOMO',
    'Contrato de prueba', NOW(), NOW()
);

-- Contrato 3: Socio 8 con Petrolera 1
INSERT IGNORE INTO contratos_socio (
    socio_id, petrolera_id, activo, fecha_vigencia_desde,
    tipo_contrato, estado, tipo_solicitante,
    observaciones, created_at, updated_at
) VALUES (
    8, 1, TRUE, CURDATE() - INTERVAL 1 MONTH,
    'Contrato General', 'COMPLETADO', 'AUTONOMO',
    'Contrato de prueba', NOW(), NOW()
);

-- Contrato 4: Socio 8 con Petrolera 4
INSERT IGNORE INTO contratos_socio (
    socio_id, petrolera_id, activo, fecha_vigencia_desde,
    tipo_contrato, estado, tipo_solicitante,
    observaciones, created_at, updated_at
) VALUES (
    8, 4, TRUE, CURDATE() - INTERVAL 2 MONTH,
    'Contrato Profesional', 'COMPLETADO', 'AUTONOMO',
    'Contrato de prueba', NOW(), NOW()
);

-- ========================================
-- PARTE 3: VERIFICACIÓN
-- ========================================

SELECT 'VERIFICACIÓN DE CAMBIOS' AS info;
SELECT '========================' AS separator;

-- Ver estructura de solicitudes_contrato
SELECT 'Columnas nullable en solicitudes_contrato:' AS info;
SELECT COLUMN_NAME, IS_NULLABLE, DATA_TYPE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'atg'
  AND TABLE_NAME = 'solicitudes_contrato'
  AND COLUMN_NAME IN ('tipo_contrato_id', 'plantilla_id');

-- Ver contratos activos
SELECT 'Contratos activos por socio:' AS info;
SELECT socio_id, petrolera_id, activo, tipo_contrato, estado
FROM contratos_socio
WHERE activo = TRUE
ORDER BY socio_id, petrolera_id;

-- Contar contratos por socio
SELECT 'Resumen de contratos por socio:' AS info;
SELECT socio_id, COUNT(*) as total_contratos, SUM(activo) as contratos_activos
FROM contratos_socio
GROUP BY socio_id;

SELECT 'SCRIPT COMPLETADO EXITOSAMENTE' AS resultado;
