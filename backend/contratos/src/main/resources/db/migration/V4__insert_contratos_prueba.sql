-- Insertar contratos de prueba para poder probar la funcionalidad de BAJA
-- IMPORTANTE: Ajustar los IDs de socio_id y petrolera_id según los datos reales de tu base de datos

-- Verificar si ya existen contratos antes de insertar
-- Si la tabla está vacía, estos INSERT funcionarán

-- Contrato 1: Socio 1 con Petrolera 1
INSERT INTO contratos_socio (
    socio_id,
    petrolera_id,
    activo,
    fecha_vigencia_desde,
    tipo_contrato,
    estado,
    tipo_solicitante,
    observaciones,
    created_at,
    updated_at
) VALUES (
    1,                              -- socio_id (ajustar según tu BD)
    1,                              -- petrolera_id (ajustar según tu BD)
    TRUE,                           -- activo
    CURRENT_DATE - INTERVAL '6 months',  -- vigente desde hace 6 meses
    'Contrato General',             -- tipo_contrato
    'COMPLETADO',                   -- estado
    'AUTONOMO',                     -- tipo_solicitante
    'Contrato de prueba insertado por migración V4',
    NOW(),
    NOW()
);

-- Contrato 2: Socio 1 con Petrolera 2
INSERT INTO contratos_socio (
    socio_id,
    petrolera_id,
    activo,
    fecha_vigencia_desde,
    tipo_contrato,
    estado,
    tipo_solicitante,
    observaciones,
    created_at,
    updated_at
) VALUES (
    1,                              -- socio_id (ajustar según tu BD)
    2,                              -- petrolera_id (ajustar según tu BD)
    TRUE,                           -- activo
    CURRENT_DATE - INTERVAL '3 months',
    'Contrato Premium',
    'COMPLETADO',
    'AUTONOMO',
    'Contrato de prueba insertado por migración V4',
    NOW(),
    NOW()
);

-- Contrato 3: Socio 2 con Petrolera 1
INSERT INTO contratos_socio (
    socio_id,
    petrolera_id,
    activo,
    fecha_vigencia_desde,
    tipo_contrato,
    estado,
    tipo_solicitante,
    observaciones,
    created_at,
    updated_at
) VALUES (
    2,                              -- socio_id (ajustar según tu BD)
    1,                              -- petrolera_id (ajustar según tu BD)
    TRUE,                           -- activo
    CURRENT_DATE - INTERVAL '1 year',
    'Contrato Empresarial',
    'COMPLETADO',
    'EMPRESA',
    'Contrato de prueba insertado por migración V4',
    NOW(),
    NOW()
);

-- Contrato 4: Socio 2 con Petrolera 2 (ya dado de baja para ejemplo)
INSERT INTO contratos_socio (
    socio_id,
    petrolera_id,
    activo,
    fecha_vigencia_desde,
    fecha_vigencia_hasta,
    tipo_contrato,
    estado,
    tipo_solicitante,
    observaciones,
    created_at,
    updated_at
) VALUES (
    2,                              -- socio_id (ajustar según tu BD)
    2,                              -- petrolera_id (ajustar según tu BD)
    FALSE,                          -- activo = false (ya dado de baja)
    CURRENT_DATE - INTERVAL '2 years',
    CURRENT_DATE - INTERVAL '1 month',  -- dado de baja hace 1 mes
    'Contrato Básico',
    'COMPLETADO',
    'EMPRESA',
    'Contrato de prueba ya dado de baja - insertado por migración V4',
    NOW(),
    NOW()
);

-- Contrato 5: Socio 3 con Petrolera 1
INSERT INTO contratos_socio (
    socio_id,
    petrolera_id,
    activo,
    fecha_vigencia_desde,
    tipo_contrato,
    estado,
    tipo_solicitante,
    observaciones,
    created_at,
    updated_at
) VALUES (
    3,                              -- socio_id (ajustar según tu BD)
    1,                              -- petrolera_id (ajustar según tu BD)
    TRUE,                           -- activo
    CURRENT_DATE - INTERVAL '2 months',
    'Contrato Estándar',
    'COMPLETADO',
    'AUTONOMO',
    'Contrato de prueba insertado por migración V4',
    NOW(),
    NOW()
);

-- Nota: Si los IDs de socio o petrolera no existen, estos INSERT fallarán
-- debido a las foreign keys. En ese caso, ajusta los valores según tu BD.
