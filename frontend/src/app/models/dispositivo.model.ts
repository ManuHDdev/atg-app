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

export interface SolicitudDispositivo {
  id?: number;
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
  monto?: number;
  observaciones?: string;
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
