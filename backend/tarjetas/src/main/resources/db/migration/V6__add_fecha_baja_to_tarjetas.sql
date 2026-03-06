-- Migración: Añadir campo fecha_baja a tarjetas
-- Fecha: 2026-01-22
-- Descripción: Añade columna fecha_baja para registrar cuándo se dio de baja una tarjeta

ALTER TABLE tarjetas
ADD COLUMN fecha_baja DATE NULL COMMENT 'Fecha en la que se dio de baja la tarjeta';
