-- Migración: Hacer matricula NOT NULL
-- Fecha: 2026-01-22
-- Descripción: Revierte V4 - hace matricula obligatoria ya que cada tarjeta debe tener una matrícula

-- Primero, actualizar cualquier registro que tenga matricula NULL a un valor por defecto
UPDATE solicitudes_tarjetas
SET matricula = 'SIN_MATRICULA'
WHERE matricula IS NULL;

-- Ahora hacer la columna NOT NULL
ALTER TABLE solicitudes_tarjetas
MODIFY COLUMN matricula VARCHAR(20) NOT NULL;
