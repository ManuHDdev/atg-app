-- =====================================================
-- SCRIPT DE DATOS INICIALES - SISTEMA DE CONTRATOS
-- =====================================================

-- =====================================================
-- TIPOS DE CONTRATO
-- =====================================================
INSERT INTO tipos_contrato (codigo, nombre, descripcion, activo, fecha_creacion, fecha_actualizacion)
VALUES
    ('ALTA', 'Alta de Contrato', 'Nueva solicitud de contrato con petrolera', true, NOW(), NOW()),
    ('BAJA', 'Baja de Contrato', 'Cancelación de contrato existente con petrolera', true, NOW(), NOW()),
    ('CAMBIO_CONDICIONES', 'Cambio de Condiciones', 'Modificación de condiciones en contrato existente', true, NOW(), NOW());

-- =====================================================
-- TIPOS DE SOLICITUD PETROLERA
-- =====================================================
-- Nota: Asumiendo que las petroleras ya están creadas en el microservicio Petroleras
-- Se deben ajustar los IDs según corresponda

-- MOEVE (Asumiendo ID = 1)
-- Para ALTA
INSERT INTO tipos_solicitud_petrolera (petrolera_id, tipo_contrato_id, nombre, descripcion, orden, activo, fecha_creacion, fecha_actualizacion)
VALUES
    (1, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Precio Lista', 'Contrato a precio lista MOEVE', 1, true, NOW(), NOW()),
    (1, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Precio Poste', 'Contrato a precio poste MOEVE', 2, true, NOW(), NOW()),
    (1, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Eurotrafic', 'Contrato Eurotrafic MOEVE', 3, true, NOW(), NOW()),
    (1, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Via T', 'Dispositivo Via T MOEVE', 4, true, NOW(), NOW());

-- SOLRED (Asumiendo ID = 2)
-- Para ALTA
INSERT INTO tipos_solicitud_petrolera (petrolera_id, tipo_contrato_id, nombre, descripcion, orden, activo, fecha_creacion, fecha_actualizacion)
VALUES
    (2, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Contrato Poste', 'Contrato a precio poste SOLRED', 1, true, NOW(), NOW()),
    (2, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Contrato Profesional', 'Contrato profesional SOLRED', 2, true, NOW(), NOW()),
    (2, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Contrato Flota', 'Contrato para flotas SOLRED', 3, true, NOW(), NOW());

-- GALP (Asumiendo ID = 3)
-- Para ALTA
INSERT INTO tipos_solicitud_petrolera (petrolera_id, tipo_contrato_id, nombre, descripcion, orden, activo, fecha_creacion, fecha_actualizacion)
VALUES
    (3, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Tarifa Poste', 'Contrato tarifa poste GALP', 1, true, NOW(), NOW()),
    (3, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Tarifa Profesional', 'Contrato tarifa profesional GALP', 2, true, NOW(), NOW());

-- REPSOL (Asumiendo ID = 4)
-- Para ALTA
INSERT INTO tipos_solicitud_petrolera (petrolera_id, tipo_contrato_id, nombre, descripcion, orden, activo, fecha_creacion, fecha_actualizacion)
VALUES
    (4, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Tarjeta Gasolinera', 'Contrato tarjeta gasolinera REPSOL', 1, true, NOW(), NOW()),
    (4, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Tarjeta Profesional', 'Contrato tarjeta profesional REPSOL', 2, true, NOW(), NOW()),
    (4, (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA'), 'Via T', 'Dispositivo Via T REPSOL', 3, true, NOW(), NOW());

-- =====================================================
-- NOTAS IMPORTANTES
-- =====================================================
-- 1. Los IDs de petroleras (1, 2, 3, 4) deben corresponder con los IDs reales
--    del microservicio de Petroleras
--
-- 2. Para el tipo de contrato "BAJA", generalmente se usa el mismo PDF para
--    todas las petroleras, por lo que no se crean tipos de solicitud específicos
--
-- 3. Para "CAMBIO_CONDICIONES", se pueden agregar tipos de solicitud específicos
--    si cada petrolera requiere formularios diferentes
--
-- 4. Las plantillas de contrato se deben subir manualmente a través de la API:
--    POST /api/plantillas
--    con los siguientes parámetros:
--    - petroleraId
--    - tipoContratoId
--    - tipoSolicitudPetroleraId (opcional, para plantillas específicas)
--    - archivo PDF editable
--
-- 5. Estructura de almacenamiento de plantillas:
--    storage/plantillas/originales/
--      ├── petrolera_1/
--      │   ├── tipo_1/
--      │   │   ├── subtipo_1/
--      │   │   ├── subtipo_2/
--      │   │   └── ...
--      │   ├── tipo_2/
--      │   └── tipo_3/
--      ├── petrolera_2/
--      └── ...
--
-- 6. Las solicitudes se almacenan en:
--    storage/contratos/solicitudes/{numeroSolicitud}/
--      ├── editable.pdf
--      ├── enviado.pdf
--      ├── firmado.pdf
--      └── final.pdf

-- =====================================================
-- EJEMPLOS DE CONSULTAS ÚTILES
-- =====================================================

-- Ver todos los tipos de contrato activos:
-- SELECT * FROM tipos_contrato WHERE activo = true;

-- Ver tipos de solicitud para una petrolera específica:
-- SELECT * FROM tipos_solicitud_petrolera WHERE petrolera_id = 1 ORDER BY orden;

-- Ver tipos de solicitud para MOEVE + ALTA:
-- SELECT tsp.*
-- FROM tipos_solicitud_petrolera tsp
-- WHERE tsp.petrolera_id = 1
-- AND tsp.tipo_contrato_id = (SELECT id FROM tipos_contrato WHERE codigo = 'ALTA')
-- ORDER BY tsp.orden;

-- Ver plantillas disponibles para una combinación:
-- SELECT * FROM plantillas_contrato
-- WHERE petrolera_id = 1
-- AND tipo_contrato_id = 1
-- AND activa = true;
