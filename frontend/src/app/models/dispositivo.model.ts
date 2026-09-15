export interface Dispositivo {
  id?: number;
  socioId: number;
  socioNombre?: string;
  petroleraId: number;
  petroleraNombre?: string;
  matricula: string;
  activo: boolean;
  fechaAlta?: string;
  fechaBaja?: string;
  createdAt?: string;
}

/** Etapas del circuito del documento firmado que tienen un PDF descargable. */
export type TipoPdfSolicitudDispositivo = 'editable' | 'enviado' | 'firmado' | 'final';

export interface SolicitudDispositivo {
  id?: number;
  /** "DIS-2026-00001"; ausente en solicitudes anteriores al circuito del documento firmado. */
  numeroSolicitud?: string;
  socioId: number;
  socioNombre?: string;
  socioEmail?: string;
  empresaId?: number;
  empresaNombre?: string;
  empresaEmail?: string;
  petroleraId: number;
  petroleraNombre?: string;
  petroleraEmail?: string;
  dispositivoId?: number;
  dispositivoMatricula?: string;
  tipoSolicitud: TipoSolicitudDispositivo;
  estado: EstadoSolicitudDispositivo;
  matricula?: string;
  matriculaDestino?: string;
  /** Importe solicitado a la petrolera (solo tipo SOLICITUD_CREDITO). */
  monto?: number;
  /** Importe concedido por la petrolera (null/undefined si no hay respuesta, fue denegada o el tipo no lleva importe). */
  montoConcedido?: number;
  observaciones?: string;

  // ---- Circuito del documento firmado ----
  rutaPdfEditable?: string;
  nombrePdfEditable?: string;
  rutaPdfEnviado?: string;
  nombrePdfEnviado?: string;
  rutaPdfFirmado?: string;
  nombrePdfFirmado?: string;
  rutaPdfFinal?: string;
  nombrePdfFinal?: string;
  fechaEnvioSocio?: string;
  fechaRecepcionFirmado?: string;
  motivoRechazo?: string;

  fechaEnvioPetrolera?: string;
  fechaRespuestaPetrolera?: string;
  fechaNotificacionSocio?: string;
  respuestaPetrolera?: string;
  programadoEnvio?: boolean;
  fechaProgramadaEnvio?: string;
  correosEnviados?: string;
  createdAt?: string;
  updatedAt?: string;
}

export enum TipoSolicitudDispositivo {
  ALTA_DISPOSITIVO = 'ALTA_DISPOSITIVO',
  SOLICITUD_CREDITO = 'SOLICITUD_CREDITO',
  BAJA_DISPOSITIVO = 'BAJA_DISPOSITIVO',
  CAMBIO_MATRICULA = 'CAMBIO_MATRICULA'
}

export enum EstadoSolicitudDispositivo {
  // Circuito del documento firmado
  BORRADOR = 'BORRADOR',
  ENVIADO_SOCIO = 'ENVIADO_SOCIO',
  FIRMADO_SOCIO = 'FIRMADO_SOCIO',
  /** Estado heredado: solicitudes anteriores al circuito del documento firmado. */
  PENDIENTE = 'PENDIENTE',
  ENVIADO_PETROLERA = 'ENVIADO_PETROLERA',
  APROBADO = 'APROBADO',
  DENEGADO = 'DENEGADO',
  COMPLETADO = 'COMPLETADO'
}

export interface CrearSolicitudDispositivoDTO {
  socioId: number;
  empresaId?: number;
  petroleraId: number;
  tipoSolicitud: TipoSolicitudDispositivo;
  dispositivoId?: number;
  matricula?: string;
  matriculaDestino?: string;
  monto?: number;
  observaciones?: string;
  programadoEnvio?: boolean;
  fechaProgramadaEnvio?: string;
}
