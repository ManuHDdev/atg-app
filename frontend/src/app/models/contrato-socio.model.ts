export interface ContratoSocio {
  id?: number;
  socioId: number;
  empresaId?: number;
  tarjetaId?: number;
  plantillaId?: number;
  petroleraId?: number;
  subseccionPetroleraId?: number;
  tipoContrato?: string;
  subtipoContrato?: string;
  tipoSolicitante?: string;
  fechaHoraSolicitud?: Date;
  solicitadoPor?: string;
  estado?: EstadoContrato;
  activo: boolean;
  fechaVigenciaDesde?: string;  // ISO date string
  fechaVigenciaHasta?: string;  // ISO date string
  rutaBorrador?: string;
  rutaEnviado?: string;
  rutaFirmado?: string;
  rutaFinal?: string;
  fechaEnvioSocio?: Date;
  fechaRecepcionFirmado?: Date;
  fechaEnvioPetrolera?: Date;
  solicitudId?: number;
  observaciones?: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export enum EstadoContrato {
  BORRADOR = 'BORRADOR',
  ENVIADO_SOCIO = 'ENVIADO_SOCIO',
  FIRMADO_SOCIO = 'FIRMADO_SOCIO',
  ENVIADO_PETROLERA = 'ENVIADO_PETROLERA',
  COMPLETADO = 'COMPLETADO',
  CANCELADO = 'CANCELADO'
}
