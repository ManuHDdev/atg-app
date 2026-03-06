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
