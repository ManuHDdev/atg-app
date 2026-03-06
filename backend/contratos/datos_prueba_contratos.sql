-- ============================================================
-- Script SQL para insertar contratos de prueba
-- Ejecutar manualmente si prefieres tener control sobre los IDs
-- ============================================================

-- PASO 1: Primero, verifica qué socios y petroleras tienes disponibles
-- Ejecuta estas consultas para ver los IDs disponibles:

-- Ver socios disponibles:
-- SELECT id, nombre, nif FROM socios WHERE activo = true;

-- Ver petroleras disponibles:
-- SELECT id, nombre FROM petroleras WHERE activa = true;

-- ============================================================
-- PASO 2: Ajusta los valores de socio_id y petrolera_id según
-- los resultados del PASO 1, y ejecuta los INSERT
-- ============================================================

-- Ejemplo: Contrato activo para Socio ID=1, Petrolera ID=1
INSERT INTO contratos_socio (
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
    1,                              -- CAMBIAR: ID del socio
    1,                              -- CAMBIAR: ID de la petrolera
    TRUE,                           -- Contrato activo
    CURRENT_DATE - INTERVAL '6 months',
    'Contrato General de Prueba',
    'COMPLETADO',
    'AUTONOMO',
    'Contrato de prueba para testing de BAJA',
    NOW(),
    NOW()
);

-- Ejemplo: Otro contrato activo para el mismo socio, diferente petrolera
INSERT INTO contratos_socio (
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
    1,                              -- CAMBIAR: ID del socio
    2,                              -- CAMBIAR: ID de la petrolera
    TRUE,
    CURRENT_DATE - INTERVAL '3 months',
    'Contrato Premium de Prueba',
    'COMPLETADO',
    'AUTONOMO',
    'Contrato de prueba para testing de BAJA',
    NOW(),
    NOW()
);

-- ============================================================
-- PASO 3: Verifica que los contratos se insertaron correctamente
-- ============================================================

-- Ver todos los contratos activos:
-- SELECT id, socio_id, petrolera_id, tipo_contrato, activo, fecha_vigencia_desde
-- FROM contratos_socio
-- WHERE activo = TRUE;

-- Ver contratos de un socio específico con una petrolera específica:
-- SELECT id, tipo_contrato, activo, fecha_vigencia_desde
-- FROM contratos_socio
-- WHERE socio_id = 1 AND petrolera_id = 1 AND activo = TRUE;
