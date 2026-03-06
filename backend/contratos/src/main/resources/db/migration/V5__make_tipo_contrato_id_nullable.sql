-- Migración: Hacer tipo_contrato_id nullable en solicitudes_contrato
-- Fecha: 2026-01-23
-- Descripción: El campo tipo_contrato_id no es necesario para todos los tipos de solicitud,
--              especialmente para BAJA y CAMBIO_CONDICIONES

-- Modificar columna para permitir valores NULL
ALTER TABLE solicitudes_contrato
MODIFY COLUMN tipo_contrato_id BIGINT NULL COMMENT 'ID del tipo de contrato (opcional - solo para algunos tipos de solicitudes)';
