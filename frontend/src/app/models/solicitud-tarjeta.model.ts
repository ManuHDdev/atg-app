export type MotivoDuplicado = 'DETERIORO' | 'EXTRAVIO';

/** Etiquetas legibles del motivo de un duplicado, tal y como se muestran al usuario. */
export const MOTIVO_DUPLICADO_LABELS: Record<MotivoDuplicado, string> = {
  DETERIORO: 'Deterioro',
  EXTRAVIO: 'Extravío'
};

/** Opciones del desplegable de motivo, en el orden en que se ofrecen al usuario. */
export const MOTIVOS_DUPLICADO: { valor: MotivoDuplicado; etiqueta: string }[] = [
  { valor: 'DETERIORO', etiqueta: MOTIVO_DUPLICADO_LABELS.DETERIORO },
  { valor: 'EXTRAVIO', etiqueta: MOTIVO_DUPLICADO_LABELS.EXTRAVIO }
];

export function getMotivoDuplicadoLabel(motivo: MotivoDuplicado | undefined): string {
  return motivo ? MOTIVO_DUPLICADO_LABELS[motivo] : '';
}

/**
 * Estados de una solicitud de tarjeta. Los tres primeros son el circuito del documento
 * firmado: el impreso se genera, se manda al socio y vuelve firmado. Solo entonces la
 * solicitud se presenta a la petrolera y pasa a PENDIENTE de su respuesta.
 */
export type EstadoSolicitudTarjeta =
  | 'BORRADOR'
  | 'ENVIADO_SOCIO'
  | 'FIRMADO_SOCIO'
  | 'PENDIENTE'
  | 'APROBADA'
  | 'RECHAZADA'
  | 'TARJETA_LLEGADA'
  | 'COMPLETADA';

/** Etapas del circuito que tienen un PDF descargable. */
export type TipoPdfSolicitud = 'editable' | 'enviado' | 'firmado' | 'final';

export interface SolicitudTarjeta {
  id?: string;
  numeroSolicitud?: string;  // "TAR-2026-00001"; ausente en solicitudes anteriores al circuito de firma
  socioId: string;
  petroleraId: string;
  matricula: string;  // Obligatorio - cada solicitud debe tener matrícula
  numeroContrato?: string;  // Opcional
  tipo: 'LLEGADA' | 'ALTA' | 'BAJA' | 'DUPLICADO';
  estado: EstadoSolicitudTarjeta;
  fechaSolicitud?: Date;
  fechaProcesado?: Date;
  observaciones?: string;
  procesadoPor?: string;
  solicitadoPor?: string;  // Persona de oficina que solicita
  fechaLlegadaEstimada?: string;  // ISO date string
  fechaEntrega?: Date;
  correosEnviados?: string;
  tarjetaId?: string;  // Para solicitudes de BAJA
  motivoDuplicado?: MotivoDuplicado;  // Solo para solicitudes de DUPLICADO

  // Circuito del documento firmado
  rutaPdfEditable?: string;
  nombrePdfEditable?: string;
  rutaPdfEnviado?: string;
  nombrePdfEnviado?: string;
  rutaPdfFirmado?: string;
  nombrePdfFirmado?: string;
  rutaPdfFinal?: string;
  nombrePdfFinal?: string;
  fechaEnvioSocio?: Date;
  fechaRecepcionFirmado?: Date;
  fechaEnvioPetrolera?: Date;
  motivoRechazo?: string;

  createdAt?: Date;
  updatedAt?: Date;
}

export interface CrearSolicitudDTO {
  socioId: string;
  petroleraId: string;
  matricula: string;  // Obligatorio - cada solicitud debe tener matrícula
  numeroContrato?: string;  // Opcional
  tipo: 'LLEGADA' | 'ALTA' | 'BAJA' | 'DUPLICADO';
  observaciones?: string;
  fechaLlegadaEstimada?: string;  // ISO date string for LLEGADA type
  solicitadoPor?: string;  // Persona de oficina (requerido en frontend para ALTA)
  tarjetaId?: string;  // Para solicitudes de BAJA
  motivoDuplicado?: MotivoDuplicado;  // Obligatorio para solicitudes de DUPLICADO
}

export interface RegistrarLlegadaDTO {
  fechaLlegadaEstimada: string;  // ISO date string
  numeroContrato?: string;  // Opcional
  observaciones?: string;
}

export interface MarcarEntregadaDTO {
  fechaEntrega?: Date;
  observaciones?: string;
}

export interface AprobarBajaDTO {
  fechaBaja: string;  // ISO date string (YYYY-MM-DD)
  observaciones?: string;
}

export interface AprobarDuplicadoDTO {
  fechaRespuesta: string;  // ISO date string (YYYY-MM-DD)
  observaciones?: string;
}
