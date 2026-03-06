-- Script para hacer nullable las columnas tipo_contrato_id y plantilla_id
-- Ejecutar manualmente si Flyway no aplica las migraciones V5 y V6

USE atg;

-- Hacer tipo_contrato_id nullable
ALTER TABLE solicitudes_contrato
MODIFY COLUMN tipo_contrato_id BIGINT NULL COMMENT 'ID del tipo de contrato (opcional - solo para algunos tipos de solicitudes)';

-- Hacer plantilla_id nullable
ALTER TABLE solicitudes_contrato
MODIFY COLUMN plantilla_id BIGINT NULL COMMENT 'ID de la plantilla (opcional)';

-- Verificar los cambios
DESCRIBE solicitudes_contrato;
