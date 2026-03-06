-- Insertar tipos de contrato básicos si no existen
INSERT IGNORE INTO tipos_contrato (codigo, nombre, descripcion, activo, fecha_creacion, fecha_actualizacion)
VALUES
    ('ALTA', 'Alta de Contrato', 'Solicitud de alta de un nuevo contrato con la petrolera', true, NOW(), NOW()),
    ('BAJA', 'Baja de Contrato', 'Solicitud de baja o cancelación de un contrato existente', true, NOW(), NOW()),
    ('CAMBIO_CONDICIONES', 'Cambio de Condiciones', 'Solicitud de modificación de condiciones de un contrato existente', true, NOW(), NOW());
