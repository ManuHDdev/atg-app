-- Migración: Añadir subsecciones a petroleras y nuevos campos a contratos
-- Fecha: 2025-12-03

-- Crear tabla de subsecciones de petroleras
CREATE TABLE IF NOT EXISTS subsecciones_petrolera (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    petrolera_id BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (petrolera_id) REFERENCES petroleras(id) ON DELETE CASCADE,
    INDEX idx_petrolera_id (petrolera_id),
    INDEX idx_activa (activa),
    INDEX idx_orden (orden)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Añadir nuevos campos a contratos_socio
ALTER TABLE contratos_socio
    ADD COLUMN IF NOT EXISTS subseccion_petrolera_id BIGINT NULL AFTER petrolera_id,
    ADD COLUMN IF NOT EXISTS tipo_solicitante VARCHAR(20) NULL AFTER tipo_contrato,
    ADD COLUMN IF NOT EXISTS fecha_hora_solicitud TIMESTAMP NULL AFTER tipo_solicitante,
    ADD COLUMN IF NOT EXISTS solicitado_por VARCHAR(200) NULL AFTER fecha_hora_solicitud;

-- Hacer tarjeta_id nullable (puede ser null si es un alta nueva)
ALTER TABLE contratos_socio MODIFY COLUMN tarjeta_id BIGINT NULL;

-- Añadir índices para mejor rendimiento
ALTER TABLE contratos_socio
    ADD INDEX IF NOT EXISTS idx_subseccion_petrolera_id (subseccion_petrolera_id),
    ADD INDEX IF NOT EXISTS idx_tipo_solicitante (tipo_solicitante),
    ADD INDEX IF NOT EXISTS idx_fecha_hora_solicitud (fecha_hora_solicitud);

-- Insertar subsecciones de ejemplo para MOEVE
INSERT INTO subsecciones_petrolera (petrolera_id, nombre, codigo, orden, activa)
SELECT id, 'Precio Lista', 'PRECIO_LISTA', 1, TRUE
FROM petroleras WHERE UPPER(nombre) LIKE '%MOEVE%'
ON DUPLICATE KEY UPDATE nombre = nombre;

INSERT INTO subsecciones_petrolera (petrolera_id, nombre, codigo, orden, activa)
SELECT id, 'Precio Poste', 'PRECIO_POSTE', 2, TRUE
FROM petroleras WHERE UPPER(nombre) LIKE '%MOEVE%'
ON DUPLICATE KEY UPDATE nombre = nombre;

INSERT INTO subsecciones_petrolera (petrolera_id, nombre, codigo, orden, activa)
SELECT id, 'Eurotrafic', 'EUROTRAFIC', 3, TRUE
FROM petroleras WHERE UPPER(nombre) LIKE '%MOEVE%'
ON DUPLICATE KEY UPDATE nombre = nombre;

INSERT INTO subsecciones_petrolera (petrolera_id, nombre, codigo, orden, activa)
SELECT id, 'Via T', 'VIA_T', 4, TRUE
FROM petroleras WHERE UPPER(nombre) LIKE '%MOEVE%'
ON DUPLICATE KEY UPDATE nombre = nombre;

COMMIT;
