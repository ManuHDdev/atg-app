-- Migración: Añadir nuevos campos a solicitudes_tarjetas
-- Fecha: 2026-01-22
-- Descripción: Añade campos para fecha de llegada estimada, fecha de entrega y log de correos enviados

-- Añadir nuevas columnas a la tabla solicitudes_tarjetas
ALTER TABLE solicitudes_tarjetas
ADD COLUMN fecha_llegada_estimada DATE NULL COMMENT 'Fecha estimada de entrega/recogida de la tarjeta',
ADD COLUMN fecha_entrega DATETIME NULL COMMENT 'Fecha real de entrega de la tarjeta al socio',
ADD COLUMN correos_enviados TEXT NULL COMMENT 'Log de correos enviados para esta solicitud';

-- Actualizar estados existentes si es necesario
-- Las solicitudes de LLEGADA que están en COMPLETADA se mantienen así
-- Las solicitudes de ALTA que están en COMPLETADA pero no tienen tarjeta asociada, se mantienen en PENDIENTE
UPDATE solicitudes_tarjetas
SET estado = 'PENDIENTE'
WHERE tipo = 'ALTA'
  AND estado = 'COMPLETADA'
  AND id NOT IN (SELECT DISTINCT solicitud_id FROM tarjetas WHERE solicitud_id IS NOT NULL);

-- Índices para mejorar el rendimiento de búsquedas
CREATE INDEX idx_solicitudes_fecha_llegada ON solicitudes_tarjetas(fecha_llegada_estimada);
CREATE INDEX idx_solicitudes_fecha_entrega ON solicitudes_tarjetas(fecha_entrega);
