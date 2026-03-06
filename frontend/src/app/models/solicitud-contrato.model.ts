export interface SolicitudContrato {
  id?: number;
  numeroSolicitud?: string;
  socioId: number;
  empresaId?: number;
  tarjetaId?: number;
  petroleraId: number;
  tipoContratoId: number;
  tipoSolicitudPetroleraId?: number;
  plantillaId?: number;
  fechaHoraSolicitud?: Date;
  solicitadoPor?: string;
  esAutonomo: boolean;
  observaciones?: string;
  estado?: EstadoSolicitud;
  tipoSolicitud: TipoSolicitudContrato;
  contratoId?: number;
  subtipoNombre?: string;
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
  fechaResolucionPetrolera?: Date;
  motivoRechazo?: string;
  correosEnviados?: string;
  fechaCreacion?: Date;
  fechaActualizacion?: Date;
}

export enum EstadoSolicitud {
  BORRADOR = 'BORRADOR',
  ENVIADO_SOCIO = 'ENVIADO_SOCIO',
  FIRMADO_SOCIO = 'FIRMADO_SOCIO',
  ENVIADO_PETROLERA = 'ENVIADO_PETROLERA',
  ACEPTADA_PETROLERA = 'ACEPTADA_PETROLERA',
  RECHAZADA_PETROLERA = 'RECHAZADA_PETROLERA'
}

export enum TipoSolicitudContrato {
  NUEVO = 'NUEVO',
  BAJA = 'BAJA',
  CAMBIO_CONDICIONES = 'CAMBIO_CONDICIONES'
}

export interface CrearSolicitudDTO {
  socioId: number;
  empresaId?: number;
  tarjetaId?: number;
  petroleraId: number;
  tipoContratoId: number;
  tipoSolicitudPetroleraId?: number;
  solicitadoPor?: string;
  esAutonomo: boolean;
  tipoSolicitud: TipoSolicitudContrato;
  contratoId?: number;
  observaciones?: string;
}

export interface FiltroSolicitudesDTO {
  socioId?: number;
  petroleraId?: number;
  tipoContratoId?: number;
  estado?: EstadoSolicitud;
  fechaDesde?: Date;
  fechaHasta?: Date;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}
