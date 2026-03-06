-- Migración: Añadir nuevas plantillas de correo
-- Fecha: 2026-01-22
-- Descripción: Añade plantillas para ALTA_APROBADA, ALTA_RECHAZADA y DUPLICADO_PETROLERA

-- Plantilla: ALTA_APROBADA
-- Notifica al socio que su solicitud de alta ha sido aprobada por la petrolera
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
    'ALTA_APROBADA',
    'Tu solicitud de tarjeta ha sido aprobada',
    '<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9;">
        <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h2 style="color: #28a745; margin-top: 0;">¡Buenas noticias, {nombre}!</h2>

            <p style="color: #333; line-height: 1.6;">
                Te informamos que <strong>{nombrePetrolera}</strong> ha <strong>aprobado</strong> tu solicitud de tarjeta para el vehículo con matrícula <strong>{matricula}</strong>.
            </p>

            <div style="background-color: #e7f3e7; padding: 15px; border-left: 4px solid #28a745; margin: 20px 0;">
                <p style="margin: 0; color: #333;">
                    <strong>Estado:</strong> Aprobada<br>
                    <strong>Matrícula:</strong> {matricula}<br>
                    <strong>Fecha de aprobación:</strong> {fecha}
                </p>
            </div>

            <p style="color: #333; line-height: 1.6;">
                En breve recibirás la tarjeta física. Te notificaremos cuando llegue a nuestras oficinas para que puedas recogerla o proceder con el envío a tu domicilio.
            </p>

            <p style="color: #666; font-size: 14px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd;">
                Si tienes alguna pregunta, no dudes en contactarnos.<br>
                <strong>ATG - Asociación de Transportistas</strong>
            </p>
        </div>
    </div>',
    '["nombre", "nif", "email", "telefono", "matricula", "numeroContrato", "fecha", "nombrePetrolera", "emailPetrolera", "provincia"]',
    1,
    NOW(),
    NOW()
);

-- Plantilla: ALTA_RECHAZADA
-- Notifica al socio que su solicitud de alta ha sido rechazada
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
    'ALTA_RECHAZADA',
    'Información sobre tu solicitud de tarjeta',
    '<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9;">
        <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h2 style="color: #dc3545; margin-top: 0;">Actualización de tu solicitud</h2>

            <p style="color: #333; line-height: 1.6;">
                Estimado/a {nombre},
            </p>

            <p style="color: #333; line-height: 1.6;">
                Lamentamos informarte que <strong>{nombrePetrolera}</strong> no ha podido aprobar tu solicitud de tarjeta para el vehículo con matrícula <strong>{matricula}</strong> en este momento.
            </p>

            <div style="background-color: #f8d7da; padding: 15px; border-left: 4px solid #dc3545; margin: 20px 0;">
                <p style="margin: 0; color: #721c24;">
                    <strong>Estado:</strong> No aprobada<br>
                    <strong>Matrícula:</strong> {matricula}<br>
                    <strong>Fecha:</strong> {fecha}
                </p>
            </div>

            <p style="color: #333; line-height: 1.6;">
                Si deseas más información sobre los motivos o quieres volver a intentar la solicitud, por favor ponte en contacto con nosotros. Estaremos encantados de ayudarte y buscar alternativas.
            </p>

            <p style="color: #666; font-size: 14px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd;">
                Puedes contactarnos respondiendo a este correo o llamando a nuestras oficinas.<br>
                <strong>ATG - Asociación de Transportistas</strong>
            </p>
        </div>
    </div>',
    '["nombre", "nif", "email", "telefono", "matricula", "numeroContrato", "fecha", "nombrePetrolera", "emailPetrolera", "provincia"]',
    1,
    NOW(),
    NOW()
);

-- Plantilla: DUPLICADO_PETROLERA
-- Solicitud de duplicado de tarjeta enviada a la petrolera
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
    'DUPLICADO_PETROLERA',
    'Solicitud de duplicado de tarjeta - ATG',
    '<div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9;">
        <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
            <h2 style="color: #007bff; margin-top: 0;">Solicitud de Duplicado de Tarjeta</h2>

            <p style="color: #333; line-height: 1.6;">
                Estimados,
            </p>

            <p style="color: #333; line-height: 1.6;">
                Desde la <strong>Asociación de Transportistas (ATG)</strong> les solicitamos el duplicado de una tarjeta para uno de nuestros socios:
            </p>

            <div style="background-color: #e7f1ff; padding: 20px; border-left: 4px solid #007bff; margin: 20px 0;">
                <h3 style="margin-top: 0; color: #007bff;">Datos del Socio</h3>
                <p style="margin: 5px 0; color: #333;">
                    <strong>Nombre:</strong> {nombre}<br>
                    <strong>NIF/CIF:</strong> {nif}<br>
                    <strong>Email:</strong> {email}<br>
                    <strong>Teléfono:</strong> {telefono}<br>
                    <strong>Provincia:</strong> {provincia}
                </p>

                <h3 style="margin-top: 20px; color: #007bff;">Datos del Vehículo</h3>
                <p style="margin: 5px 0; color: #333;">
                    <strong>Matrícula:</strong> {matricula}<br>
                    <strong>Número de contrato:</strong> {numeroContrato}
                </p>

                <h3 style="margin-top: 20px; color: #007bff;">Motivo</h3>
                <p style="margin: 5px 0; color: #333;">
                    Solicitud de duplicado de tarjeta (pérdida, robo, o deterioro)
                </p>
            </div>

            <p style="color: #333; line-height: 1.6;">
                Les agradeceríamos que procesaran esta solicitud a la mayor brevedad posible.
            </p>

            <p style="color: #333; line-height: 1.6;">
                Quedamos a su disposición para cualquier aclaración adicional.
            </p>

            <p style="color: #666; font-size: 14px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd;">
                Fecha de solicitud: {fecha}<br>
                <strong>Asociación de Transportistas (ATG)</strong>
            </p>
        </div>
    </div>',
    '["nombre", "nif", "email", "telefono", "matricula", "numeroContrato", "fecha", "nombrePetrolera", "emailPetrolera", "provincia"]',
    1,
    NOW(),
    NOW()
);
