-- Script SQL para crear las tablas de créditos y plantillas de correo
-- Base de datos: atg

USE atg;

-- Tabla de créditos
CREATE TABLE IF NOT EXISTS creditos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    socio_id BIGINT NOT NULL,
    empresa_id BIGINT,
    petrolera_id BIGINT NOT NULL,
    tipo_credito VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    monto DECIMAL(10, 2),
    observaciones TEXT,
    fecha_envio_petrolera DATETIME,
    fecha_respuesta_petrolera DATETIME,
    fecha_notificacion_socio DATETIME,
    respuesta_petrolera TEXT,
    programado_envio BOOLEAN DEFAULT FALSE,
    fecha_programada_envio DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_socio (socio_id),
    INDEX idx_petrolera (petrolera_id),
    INDEX idx_estado (estado),
    INDEX idx_tipo (tipo_credito),
    INDEX idx_programado (programado_envio, fecha_programada_envio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Agregar campos a la tabla petroleras (solo si no existen)
ALTER TABLE petroleras
ADD COLUMN email VARCHAR(255);

ALTER TABLE petroleras
ADD COLUMN dia_envio_creditos INT;

-- Tabla de plantillas de correo
CREATE TABLE IF NOT EXISTS plantillas_correo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    petrolera_id BIGINT NOT NULL,
    tipo_plantilla VARCHAR(50) NOT NULL,
    asunto VARCHAR(255) NOT NULL,
    cuerpo TEXT NOT NULL,
    variables_disponibles TEXT,
    activa BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (petrolera_id) REFERENCES petroleras(id) ON DELETE CASCADE,
    UNIQUE KEY uk_petrolera_tipo (petrolera_id, tipo_plantilla),
    INDEX idx_petrolera (petrolera_id),
    INDEX idx_tipo (tipo_plantilla)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar plantillas de ejemplo
INSERT INTO plantillas_correo (petrolera_id, tipo_plantilla, asunto, cuerpo, variables_disponibles)
VALUES
(1, 'SOLICITUD_CREDITO',
 'Solicitud de Crédito - {{socio_nombre}}',
 '<html><body><h2>Solicitud de Crédito</h2><p>Estimados señores de {{petrolera_nombre}},</p><p>Por medio de la presente, solicitamos crédito para el socio:</p><ul><li><strong>Nombre:</strong> {{socio_nombre}}</li><li><strong>Número de Socio:</strong> {{socio_numero}}</li><li><strong>Email:</strong> {{socio_email}}</li><li><strong>Teléfono:</strong> {{socio_telefono}}</li><li><strong>Empresa:</strong> {{empresa_nombre}}</li><li><strong>CIF:</strong> {{empresa_cif}}</li><li><strong>Monto solicitado:</strong> {{monto}} €</li></ul><p><strong>Observaciones:</strong><br>{{observaciones}}</p><p>Quedamos a la espera de su respuesta.</p><p>Saludos cordiales,<br>Equipo ATG</p></body></html>',
 '{"socio_nombre":"Nombre del socio","socio_numero":"Número de socio","socio_email":"Email del socio","socio_telefono":"Teléfono del socio","empresa_nombre":"Nombre de la empresa","empresa_cif":"CIF de la empresa","petrolera_nombre":"Nombre de la petrolera","monto":"Monto solicitado","observaciones":"Observaciones adicionales","fecha_solicitud":"Fecha de la solicitud"}')
ON DUPLICATE KEY UPDATE cuerpo = VALUES(cuerpo);

INSERT INTO plantillas_correo (petrolera_id, tipo_plantilla, asunto, cuerpo, variables_disponibles)
VALUES
(1, 'AMPLIACION_CREDITO',
 'Solicitud de Ampliación de Crédito - {{socio_nombre}}',
 '<html><body><h2>Solicitud de Ampliación de Crédito</h2><p>Estimados señores de {{petrolera_nombre}},</p><p>Por medio de la presente, solicitamos ampliación de crédito para el socio:</p><ul><li><strong>Nombre:</strong> {{socio_nombre}}</li><li><strong>Número de Socio:</strong> {{socio_numero}}</li><li><strong>Email:</strong> {{socio_email}}</li><li><strong>Empresa:</strong> {{empresa_nombre}}</li><li><strong>Monto de ampliación:</strong> {{monto}} €</li></ul><p><strong>Observaciones:</strong><br>{{observaciones}}</p><p>Quedamos a la espera de su respuesta.</p><p>Saludos cordiales,<br>Equipo ATG</p></body></html>',
 '{"socio_nombre":"Nombre del socio","socio_numero":"Número de socio","socio_email":"Email del socio","empresa_nombre":"Nombre de la empresa","petrolera_nombre":"Nombre de la petrolera","monto":"Monto de ampliación","observaciones":"Observaciones adicionales"}')
ON DUPLICATE KEY UPDATE cuerpo = VALUES(cuerpo);

INSERT INTO plantillas_correo (petrolera_id, tipo_plantilla, asunto, cuerpo, variables_disponibles)
VALUES
(1, 'DEVOLUCION_AVAL',
 'Solicitud de Devolución de Aval - {{socio_nombre}}',
 '<html><body><h2>Solicitud de Devolución de Aval</h2><p>Estimados señores de {{petrolera_nombre}},</p><p>Por medio de la presente, solicitamos la devolución del aval del socio:</p><ul><li><strong>Nombre:</strong> {{socio_nombre}}</li><li><strong>Número de Socio:</strong> {{socio_numero}}</li><li><strong>Email:</strong> {{socio_email}}</li><li><strong>Empresa:</strong> {{empresa_nombre}}</li></ul><p><strong>Observaciones:</strong><br>{{observaciones}}</p><p>Quedamos a la espera de su respuesta.</p><p>Saludos cordiales,<br>Equipo ATG</p></body></html>',
 '{"socio_nombre":"Nombre del socio","socio_numero":"Número de socio","socio_email":"Email del socio","empresa_nombre":"Nombre de la empresa","petrolera_nombre":"Nombre de la petrolera","observaciones":"Observaciones adicionales"}')
ON DUPLICATE KEY UPDATE cuerpo = VALUES(cuerpo);

-- Comentarios sobre el uso
/*
TIPOS DE CRÉDITO:
- SOLICITUD_CREDITO: Nueva solicitud de crédito
- AMPLIACION_CREDITO: Ampliación de crédito existente
- DEVOLUCION_AVAL: Devolución de aval

ESTADOS DE CRÉDITO:
- PENDIENTE: Creado, esperando envío
- ENVIADO_PETROLERA: Enviado a la petrolera
- APROBADO: Aprobado por la petrolera
- DENEGADO: Denegado por la petrolera
- COMPLETADO: Proceso completado (socio notificado)

VARIABLES DISPONIBLES EN PLANTILLAS:
{{socio_nombre}}, {{socio_numero}}, {{socio_email}}, {{socio_telefono}}
{{empresa_nombre}}, {{empresa_cif}}, {{empresa_email}}
{{petrolera_nombre}}
{{tipo_credito}}, {{monto}}, {{observaciones}}, {{fecha_solicitud}}
*/
