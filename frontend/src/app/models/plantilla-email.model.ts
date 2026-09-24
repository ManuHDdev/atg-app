export interface PlantillaEmail {
  id?: number;
  nombre: string;
  asunto: string;
  cuerpo: string;
  tipoEvento: TipoEventoEmail;
  activa: boolean;
  variables?: string;
  petroleraId?: number;
  petroleraNombre?: string;
  createdAt?: string;
  updatedAt?: string;
}

export enum TipoEventoEmail {
  SOLICITUD_CREDITO = 'SOLICITUD_CREDITO',
  AMPLIACION_CREDITO = 'AMPLIACION_CREDITO',
  DEVOLUCION_AVAL = 'DEVOLUCION_AVAL',
  NOTIF_SOCIO_CREADO = 'NOTIF_SOCIO_CREADO',
  NOTIF_SOCIO_ENVIADO = 'NOTIF_SOCIO_ENVIADO',
  NOTIF_SOCIO_RESULTADO = 'NOTIF_SOCIO_RESULTADO',
  CONTRATO_PETROLERA = 'CONTRATO_PETROLERA',
  NOTIF_SOCIO_CONTRATO_CREADO = 'NOTIF_SOCIO_CONTRATO_CREADO',
  NOTIF_SOCIO_CONTRATO_ENVIADO = 'NOTIF_SOCIO_CONTRATO_ENVIADO',
  NOTIF_SOCIO_CONTRATO_RESULTADO = 'NOTIF_SOCIO_CONTRATO_RESULTADO',
  ALTA_DISPOSITIVO = 'ALTA_DISPOSITIVO',
  SOLICITUD_CREDITO_DISPOSITIVO = 'SOLICITUD_CREDITO_DISPOSITIVO',
  BAJA_DISPOSITIVO = 'BAJA_DISPOSITIVO',
  CAMBIO_MATRICULA = 'CAMBIO_MATRICULA',
  // Circuito del documento firmado de dispositivos: el impreso que se manda al socio para
  // su firma y el envío a la petrolera con ese mismo impreso ya firmado.
  DOCUMENTO_SOCIO_DISPOSITIVO = 'DOCUMENTO_SOCIO_DISPOSITIVO',
  DOCUMENTO_PETROLERA_DISPOSITIVO = 'DOCUMENTO_PETROLERA_DISPOSITIVO',
  NOTIF_SOCIO_DISP_CREADO = 'NOTIF_SOCIO_DISP_CREADO',
  NOTIF_SOCIO_DISP_ENVIADO = 'NOTIF_SOCIO_DISP_ENVIADO',
  NOTIF_SOCIO_DISP_RESULTADO = 'NOTIF_SOCIO_DISP_RESULTADO'
}

export interface CrearPlantillaEmailDTO {
  nombre: string;
  asunto: string;
  cuerpo: string;
  tipoEvento: TipoEventoEmail;
  activa: boolean;
  petroleraId?: number;
}

// Variables disponibles para cada tipo de plantilla
export const VARIABLES_POR_TIPO: Record<TipoEventoEmail, string[]> = {
  [TipoEventoEmail.SOLICITUD_CREDITO]: [
    '{{socio_nombre}}', '{{socio_email}}', '{{socio_telefono}}', '{{socio_numero}}',
    '{{empresa_nombre}}', '{{empresa_cif}}',
    '{{petrolera_nombre}}', '{{tipo_credito}}', '{{monto}}',
    '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.AMPLIACION_CREDITO]: [
    '{{socio_nombre}}', '{{socio_email}}', '{{socio_telefono}}', '{{socio_numero}}',
    '{{empresa_nombre}}', '{{empresa_cif}}',
    '{{petrolera_nombre}}', '{{tipo_credito}}', '{{monto}}',
    '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.DEVOLUCION_AVAL]: [
    '{{socio_nombre}}', '{{socio_email}}', '{{socio_telefono}}', '{{socio_numero}}',
    '{{empresa_nombre}}', '{{empresa_cif}}',
    '{{petrolera_nombre}}', '{{tipo_credito}}',
    '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_CREADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_credito}}', '{{monto}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_ENVIADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_credito}}', '{{monto}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_RESULTADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_credito}}', '{{monto}}',
    '{{estado}}', '{{respuesta_petrolera}}'
  ],
  [TipoEventoEmail.CONTRATO_PETROLERA]: [
    '{{socio_nombre}}', '{{socio_nif}}', '{{socio_numero}}',
    '{{empresa_nombre}}', '{{empresa_cif}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{subtipo}}',
    '{{numero_solicitud}}', '{{numero_contrato}}',
    '{{matricula}}', '{{agrupacion}}',
    '{{fecha_actual}}', '{{observaciones}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_CONTRATO_CREADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{subtipo}}',
    '{{numero_solicitud}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_CONTRATO_ENVIADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{subtipo}}',
    '{{numero_solicitud}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_CONTRATO_RESULTADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{subtipo}}',
    '{{numero_solicitud}}', '{{estado}}', '{{respuesta_petrolera}}'
  ],
  [TipoEventoEmail.ALTA_DISPOSITIVO]: [
    '{{socio_nombre}}', '{{socio_nif}}', '{{socio_numero}}',
    '{{petrolera_nombre}}', '{{matricula}}',
    '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.SOLICITUD_CREDITO_DISPOSITIVO]: [
    '{{socio_nombre}}', '{{socio_nif}}', '{{socio_numero}}',
    '{{petrolera_nombre}}', '{{matricula}}', '{{monto}}',
    '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.BAJA_DISPOSITIVO]: [
    '{{socio_nombre}}', '{{socio_nif}}', '{{socio_numero}}',
    '{{petrolera_nombre}}', '{{matricula}}',
    '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.CAMBIO_MATRICULA]: [
    '{{socio_nombre}}', '{{socio_nif}}', '{{socio_numero}}',
    '{{petrolera_nombre}}', '{{matricula}}', '{{matricula_destino}}',
    '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.DOCUMENTO_SOCIO_DISPOSITIVO]: [
    '{{socio_nombre}}', '{{socio_email}}', '{{socio_nif}}', '{{socio_numero}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{matricula}}',
    '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.DOCUMENTO_PETROLERA_DISPOSITIVO]: [
    '{{socio_nombre}}', '{{socio_email}}', '{{socio_nif}}', '{{socio_numero}}',
    '{{empresa_nombre}}', '{{empresa_cif}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{matricula}}', '{{matricula_destino}}',
    '{{monto}}', '{{observaciones}}', '{{fecha_solicitud}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_DISP_CREADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{matricula}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_DISP_ENVIADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{matricula}}'
  ],
  [TipoEventoEmail.NOTIF_SOCIO_DISP_RESULTADO]: [
    '{{socio_nombre}}', '{{socio_email}}',
    '{{petrolera_nombre}}', '{{tipo_solicitud}}', '{{matricula}}',
    '{{estado}}', '{{respuesta_petrolera}}'
  ]
};

/**
 * Etiqueta de cada tipo de evento. Al estar tipado como Record<TipoEventoEmail, string>,
 * añadir un valor al enum sin añadirlo aquí rompe la compilación: es lo que evita que la
 * pantalla de plantillas se quede sin ofrecer un tipo que el backend sí busca al enviar.
 */
export const TIPO_EVENTO_EMAIL_LABELS: Record<TipoEventoEmail, string> = {
  [TipoEventoEmail.SOLICITUD_CREDITO]: 'Crédito: Solicitud',
  [TipoEventoEmail.AMPLIACION_CREDITO]: 'Crédito: Ampliación',
  [TipoEventoEmail.DEVOLUCION_AVAL]: 'Crédito: Devolución de Aval',
  [TipoEventoEmail.NOTIF_SOCIO_CREADO]: 'Crédito: Notif. Trámite Registrado',
  [TipoEventoEmail.NOTIF_SOCIO_ENVIADO]: 'Crédito: Notif. Enviado a Petrolera',
  [TipoEventoEmail.NOTIF_SOCIO_RESULTADO]: 'Crédito: Notif. Resultado',
  [TipoEventoEmail.CONTRATO_PETROLERA]: 'Contrato: Email a Petrolera',
  [TipoEventoEmail.NOTIF_SOCIO_CONTRATO_CREADO]: 'Contrato: Notif. Socio - Solicitud Registrada',
  [TipoEventoEmail.NOTIF_SOCIO_CONTRATO_ENVIADO]: 'Contrato: Notif. Socio - Contrato Enviado',
  [TipoEventoEmail.NOTIF_SOCIO_CONTRATO_RESULTADO]: 'Contrato: Notif. Socio - Resultado',
  [TipoEventoEmail.ALTA_DISPOSITIVO]: 'Dispositivo: Alta',
  [TipoEventoEmail.SOLICITUD_CREDITO_DISPOSITIVO]: 'Dispositivo: Solicitud de Crédito',
  [TipoEventoEmail.BAJA_DISPOSITIVO]: 'Dispositivo: Baja',
  [TipoEventoEmail.CAMBIO_MATRICULA]: 'Dispositivo: Cambio de Matrícula',
  [TipoEventoEmail.DOCUMENTO_SOCIO_DISPOSITIVO]: 'Dispositivo: Envío al Socio para Firma',
  [TipoEventoEmail.DOCUMENTO_PETROLERA_DISPOSITIVO]: 'Dispositivo: Envío del Firmado a la Petrolera',
  [TipoEventoEmail.NOTIF_SOCIO_DISP_CREADO]: 'Dispositivo: Notif. Solicitud Registrada',
  [TipoEventoEmail.NOTIF_SOCIO_DISP_ENVIADO]: 'Dispositivo: Notif. Enviado a Petrolera',
  [TipoEventoEmail.NOTIF_SOCIO_DISP_RESULTADO]: 'Dispositivo: Notif. Resultado'
};

/** Opciones del desplegable y del filtro de tipo, derivadas del enum: nunca se escriben a mano. */
export const TIPOS_EVENTO_EMAIL: { value: TipoEventoEmail; label: string }[] =
  Object.values(TipoEventoEmail).map(value => ({ value, label: TIPO_EVENTO_EMAIL_LABELS[value] }));
