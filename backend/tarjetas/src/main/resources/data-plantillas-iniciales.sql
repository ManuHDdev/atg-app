-- Script SQL para insertar plantillas iniciales de correo
-- Ejecutar después de que Hibernate cree las tablas

-- PLANTILLA 1: LLEGADA_MADRID
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
  'LLEGADA_MADRID',
  'Tu tarjeta está disponible para recoger - ATG',
  '<html>
    <body style="font-family: Arial, sans-serif; color: #333;">
      <h2 style="color: #0066cc;">Tu tarjeta ha llegado</h2>
      <p>Hola <strong>{nombre}</strong>,</p>
      <p>Te informamos que tu tarjeta para el vehículo con matrícula <strong>{matricula}</strong> ha llegado a nuestras oficinas y está disponible para su recogida.</p>
      <p>Puedes pasar a recogerla en nuestro horario de atención:</p>
      <ul>
        <li>Lunes a Viernes: 9:00 - 14:00 y 16:00 - 19:00</li>
        <li>Sábados: 10:00 - 13:00</li>
      </ul>
      <p>Por favor, trae contigo tu DNI/NIE para identificarte.</p>
      <p>Si tienes alguna duda, no dudes en contactarnos.</p>
      <br>
      <p>Saludos cordiales,<br>
      <strong>Equipo ATG</strong></p>
    </body>
  </html>',
  '{"nombre","matricula","fecha","telefono","email"}',
  TRUE,
  NOW(),
  NOW()
);

-- PLANTILLA 2: LLEGADA_FUERA (fuera de Madrid)
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
  'LLEGADA_FUERA',
  'Tu tarjeta será enviada a tu domicilio - ATG',
  '<html>
    <body style="font-family: Arial, sans-serif; color: #333;">
      <h2 style="color: #0066cc;">Tu tarjeta ha llegado</h2>
      <p>Hola <strong>{nombre}</strong>,</p>
      <p>Te informamos que tu tarjeta para el vehículo con matrícula <strong>{matricula}</strong> ha llegado a nuestras oficinas.</p>
      <p>Como resides en <strong>{provincia}</strong>, procederemos a enviarla a tu domicilio registrado mediante correo certificado en los próximos 3-5 días laborables.</p>
      <p>Recibirás un aviso de entrega cuando el envío esté en camino.</p>
      <p>Si necesitas modificar la dirección de envío o tienes alguna duda, por favor contáctanos lo antes posible:</p>
      <ul>
        <li>Teléfono: 91 XXX XX XX</li>
        <li>Email: info@atg.com</li>
      </ul>
      <br>
      <p>Saludos cordiales,<br>
      <strong>Equipo ATG</strong></p>
    </body>
  </html>',
  '{"nombre","matricula","provincia","fecha","telefono","email"}',
  TRUE,
  NOW(),
  NOW()
);

-- PLANTILLA 3: ALTA_SOCIO
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
  'ALTA_SOCIO',
  'Solicitud de alta de tarjeta en proceso - ATG',
  '<html>
    <body style="font-family: Arial, sans-serif; color: #333;">
      <h2 style="color: #0066cc;">Solicitud de alta de tarjeta recibida</h2>
      <p>Hola <strong>{nombre}</strong>,</p>
      <p>Hemos recibido tu solicitud de alta de tarjeta para el vehículo con matrícula <strong>{matricula}</strong>.</p>
      <p>Tu solicitud está siendo tramitada con la petrolera <strong>{nombrePetrolera}</strong>. Este proceso puede tomar entre 7 y 15 días laborables.</p>
      <p><strong>Detalles de la solicitud:</strong></p>
      <ul>
        <li>Matrícula: <strong>{matricula}</strong></li>
        <li>Petrolera: <strong>{nombrePetrolera}</strong></li>
        <li>Fecha de solicitud: <strong>{fecha}</strong></li>
      </ul>
      <p>Te notificaremos cuando la tarjeta llegue a nuestras oficinas o esté lista para su envío.</p>
      <p>Si tienes alguna pregunta, no dudes en contactarnos.</p>
      <br>
      <p>Saludos cordiales,<br>
      <strong>Equipo ATG</strong></p>
    </body>
  </html>',
  '{"nombre","nif","matricula","nombrePetrolera","fecha","telefono","email"}',
  TRUE,
  NOW(),
  NOW()
);

-- PLANTILLA 4: ALTA_PETROLERA
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
  'ALTA_PETROLERA',
  'Solicitud de nueva tarjeta para socio - ATG',
  '<html>
    <body style="font-family: Arial, sans-serif; color: #333;">
      <h2 style="color: #0066cc;">Nueva solicitud de tarjeta</h2>
      <p>Estimados,</p>
      <p>Les solicitamos el alta de una nueva tarjeta con los siguientes datos:</p>
      <p><strong>Datos del socio:</strong></p>
      <ul>
        <li>Nombre: <strong>{nombre}</strong></li>
        <li>NIF/NIE: <strong>{nif}</strong></li>
        <li>Teléfono: <strong>{telefono}</strong></li>
        <li>Email: <strong>{email}</strong></li>
      </ul>
      <p><strong>Datos del vehículo:</strong></p>
      <ul>
        <li>Matrícula: <strong>{matricula}</strong></li>
      </ul>
      <p>Fecha de solicitud: <strong>{fecha}</strong></p>
      <p>Por favor, procedan con el trámite de alta y notifiquen cualquier documentación adicional que sea necesaria.</p>
      <p>Quedamos a la espera de su confirmación.</p>
      <br>
      <p>Atentamente,<br>
      <strong>Departamento de Gestión - ATG</strong></p>
    </body>
  </html>',
  '{"nombre","nif","matricula","telefono","email","fecha"}',
  TRUE,
  NOW(),
  NOW()
);

-- PLANTILLA 5: BAJA_SOCIO
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
  'BAJA_SOCIO',
  'Confirmación de baja de tarjeta - ATG',
  '<html>
    <body style="font-family: Arial, sans-serif; color: #333;">
      <h2 style="color: #0066cc;">Baja de tarjeta procesada</h2>
      <p>Hola <strong>{nombre}</strong>,</p>
      <p>Te confirmamos que hemos procesado la baja de la tarjeta del vehículo con matrícula <strong>{matricula}</strong>.</p>
      <p>El trámite ha sido gestionado con la petrolera <strong>{nombrePetrolera}</strong> y la tarjeta quedará desactivada en un plazo máximo de 48 horas.</p>
      <p><strong>Detalles de la baja:</strong></p>
      <ul>
        <li>Matrícula: <strong>{matricula}</strong></li>
        <li>Petrolera: <strong>{nombrePetrolera}</strong></li>
        <li>Fecha de baja: <strong>{fecha}</strong></li>
      </ul>
      <p><strong>Importante:</strong> Si posees la tarjeta física, por favor destrúyela cortándola por la mitad.</p>
      <p>Si necesitas volver a dar de alta esta tarjeta en el futuro, contacta con nosotros.</p>
      <br>
      <p>Saludos cordiales,<br>
      <strong>Equipo ATG</strong></p>
    </body>
  </html>',
  '{"nombre","matricula","nombrePetrolera","fecha","telefono","email"}',
  TRUE,
  NOW(),
  NOW()
);

-- PLANTILLA 6: DUPLICADO_SOCIO
INSERT INTO plantillas_tarjetas (tipo, asunto, cuerpo, variables_disponibles, activa, created_at, updated_at)
VALUES (
  'DUPLICADO_SOCIO',
  'Solicitud de duplicado de tarjeta en proceso - ATG',
  '<html>
    <body style="font-family: Arial, sans-serif; color: #333;">
      <h2 style="color: #0066cc;">Solicitud de duplicado recibida</h2>
      <p>Hola <strong>{nombre}</strong>,</p>
      <p>Hemos recibido tu solicitud de duplicado de tarjeta para el vehículo con matrícula <strong>{matricula}</strong>.</p>
      <p>Estamos gestionando el trámite con la petrolera <strong>{nombrePetrolera}</strong>. El proceso de emisión de duplicados puede tomar entre 10 y 20 días laborables.</p>
      <p><strong>Detalles de la solicitud:</strong></p>
      <ul>
        <li>Matrícula: <strong>{matricula}</strong></li>
        <li>Petrolera: <strong>{nombrePetrolera}</strong></li>
        <li>Fecha de solicitud: <strong>{fecha}</strong></li>
      </ul>
      <p><strong>Nota importante:</strong> Si has encontrado la tarjeta original después de solicitar el duplicado, por favor contacta con nosotros inmediatamente.</p>
      <p>Te notificaremos cuando el duplicado llegue a nuestras oficinas.</p>
      <br>
      <p>Saludos cordiales,<br>
      <strong>Equipo ATG</strong></p>
    </body>
  </html>',
  '{"nombre","matricula","nombrePetrolera","fecha","telefono","email"}',
  TRUE,
  NOW(),
  NOW()
);
