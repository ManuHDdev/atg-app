-- Migración: Añadir nuevos campos a solicitudes_tarjetas
-- Fecha: 2026-01-22
-- Descripción: Añade numeroContrato, solicitadoPor, tarjetaId y hace matricula opcional

-- Añadir nuevas columnas
ALTER TABLE solicitudes_tarjetas
ADD COLUMN numero_contrato VARCHAR(50) NULL COMMENT 'Número de contrato de la tarjeta',
ADD COLUMN solicitado_por VARCHAR(100) NULL COMMENT 'Persona de oficina que solicita (para ALTA)',
ADD COLUMN tarjeta_id BIGINT NULL COMMENT 'ID de tarjeta a dar de baja (para BAJA)';

-- Hacer que matricula sea opcional (eliminar restricción NOT NULL)
ALTER TABLE solicitudes_tarjetas
MODIFY COLUMN matricula VARCHAR(20) NULL;

-- Añadir índice para tarjeta_id
CREATE INDEX idx_solicitudes_tarjeta_id ON solicitudes_tarjetas(tarjeta_id);
