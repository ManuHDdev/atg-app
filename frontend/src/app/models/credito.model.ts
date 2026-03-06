export interface Credito {
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
  tipoCredito: TipoCredito;
  estado: EstadoCredito;
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

export enum TipoCredito {
  SOLICITUD_CREDITO = 'SOLICITUD_CREDITO',
  AMPLIACION_CREDITO = 'AMPLIACION_CREDITO',
  DEVOLUCION_AVAL = 'DEVOLUCION_AVAL'
}

export enum EstadoCredito {
  PENDIENTE = 'PENDIENTE',
  ENVIADO_PETROLERA = 'ENVIADO_PETROLERA',
  APROBADO = 'APROBADO',
  DENEGADO = 'DENEGADO',
  COMPLETADO_APROBADO = 'COMPLETADO_APROBADO',
  COMPLETADO_DENEGADO = 'COMPLETADO_DENEGADO'
}

export interface CrearCreditoDTO {
  socioId: number;
  empresaId?: number;
  petroleraId: number;
  tipoCredito: TipoCredito;
  monto?: number;
  observaciones?: string;
  programadoEnvio?: boolean;
  fechaProgramadaEnvio?: string;
}
