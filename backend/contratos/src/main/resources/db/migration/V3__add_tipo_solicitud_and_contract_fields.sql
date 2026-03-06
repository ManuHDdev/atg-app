-- Migración: Añadir tipo_solicitud y campos de contrato
-- Fecha: 2026-01-22
-- Descripción: Añade campos para diferenciar tipos de solicitud (NUEVO, BAJA, CAMBIO_CONDICIONES) y control de vigencia

-- Añadir tipo_solicitud a solicitudes_contrato
ALTER TABLE solicitudes_contrato
ADD COLUMN tipo_solicitud VARCHAR(50) NOT NULL DEFAULT 'NUEVO' COMMENT 'Tipo de solicitud: NUEVO, BAJA, CAMBIO_CONDICIONES';

-- Añadir contrato_id a solicitudes_contrato para solicitudes de BAJA/CAMBIO
ALTER TABLE solicitudes_contrato
ADD COLUMN contrato_id BIGINT NULL COMMENT 'ID del contrato para solicitudes de BAJA o CAMBIO_CONDICIONES',
ADD CONSTRAINT fk_solicitud_contrato_id FOREIGN KEY (contrato_id) REFERENCES contratos_socio(id);

-- Añadir campo activo a contratos_socio
ALTER TABLE contratos_socio
ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Indica si el contrato está activo';

-- Añadir fechas de vigencia a contratos_socio
ALTER TABLE contratos_socio
ADD COLUMN fecha_vigencia_desde DATE NULL COMMENT 'Fecha de inicio de vigencia del contrato',
ADD COLUMN fecha_vigencia_hasta DATE NULL COMMENT 'Fecha de fin de vigencia del contrato (para bajas)';

-- Crear índices
CREATE INDEX idx_solicitudes_tipo_solicitud ON solicitudes_contrato(tipo_solicitud);
CREATE INDEX idx_solicitudes_contrato_id ON solicitudes_contrato(contrato_id);
CREATE INDEX idx_contratos_activo ON contratos_socio(activo);
