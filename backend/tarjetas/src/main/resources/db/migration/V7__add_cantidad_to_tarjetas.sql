-- Migración: Añadir campo cantidad a tarjetas
-- Fecha: 2026-01-22
-- Descripción: Añade columna cantidad para llevar control del número de tarjetas físicas con la misma matrícula

-- Añadir columna cantidad con valor por defecto 1
ALTER TABLE tarjetas
ADD COLUMN cantidad INT NOT NULL DEFAULT 1 COMMENT 'Número de tarjetas físicas (se incrementa con duplicados)';

-- Actualizar todas las tarjetas existentes a cantidad = 1
UPDATE tarjetas
SET cantidad = 1
WHERE cantidad IS NULL OR cantidad = 0;
