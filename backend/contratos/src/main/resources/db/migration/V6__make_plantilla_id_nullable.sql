-- Migración: Hacer plantilla_id nullable en solicitudes_contrato
-- Fecha: 2026-01-23
-- Descripción: El campo plantilla_id no siempre está disponible al crear la solicitud

-- Modificar columna para permitir valores NULL
ALTER TABLE solicitudes_contrato
MODIFY COLUMN plantilla_id BIGINT NULL COMMENT 'ID de la plantilla (opcional)';
