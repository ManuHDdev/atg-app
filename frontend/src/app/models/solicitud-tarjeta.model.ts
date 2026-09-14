export interface SolicitudTarjeta {
  id?: string;
  socioId: string;
  petroleraId: string;
  matricula: string;  // Obligatorio - cada solicitud debe tener matrícula
  numeroContrato?: string;  // Opcional
  tipo: 'LLEGADA' | 'ALTA' | 'BAJA' | 'DUPLICADO';
  estado: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'TARJETA_LLEGADA' | 'COMPLETADA';
  fechaSolicitud?: Date;
  fechaProcesado?: Date;
  observaciones?: string;
  procesadoPor?: string;
  solicitadoPor?: string;  // Persona de oficina que solicita
  fechaLlegadaEstimada?: string;  // ISO date string
  fechaEntrega?: Date;
  correosEnviados?: string;
  tarjetaId?: string;  // Para solicitudes de BAJA
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
