export type TipoIncidencia = 'BUG' | 'MEJORA' | 'SUGERENCIA';
export type Prioridad = 'ALTA' | 'MEDIA' | 'BAJA';
export type EstadoIncidencia =
  | 'NUEVA'
  | 'EN_REVISION'
  | 'EN_DESARROLLO'
  | 'PENDIENTE_DEPLOY'
  | 'EN_PRODUCCION'
  | 'DESCARTADA';

export const ESTADOS_INCIDENCIA: EstadoIncidencia[] = [
  'NUEVA', 'EN_REVISION', 'EN_DESARROLLO', 'PENDIENTE_DEPLOY', 'EN_PRODUCCION', 'DESCARTADA'
];

export const ESTADO_LABELS: Record<EstadoIncidencia, string> = {
  NUEVA: 'Nueva',
  EN_REVISION: 'En revisión',
  EN_DESARROLLO: 'En desarrollo',
  PENDIENTE_DEPLOY: 'Pendiente de deploy',
  EN_PRODUCCION: 'En producción',
  DESCARTADA: 'Descartada'
};

export const TIPO_LABELS: Record<TipoIncidencia, string> = {
  BUG: 'Bug',
  MEJORA: 'Mejora',
  SUGERENCIA: 'Sugerencia'
};

export interface Comentario {
  id: number;
  texto: string;
  autor: string;
  fecha: string;
}

export interface IncidenciaResumen {
  id: number;
  titulo: string;
  tipo: TipoIncidencia;
  prioridad: Prioridad;
  estado: EstadoIncidencia;
  autor: string;
  fechaCreacion: string;
  numComentarios: number;
}

export interface Incidencia extends IncidenciaResumen {
  descripcion: string;
  notasDeveloper: string | null;
  fechaActualizacion: string;
  comentarios: Comentario[];
}

export interface CreateIncidenciaRequest {
  titulo: string;
  descripcion: string;
  tipo: TipoIncidencia;
  prioridad: Prioridad;
}

export interface CambiarEstadoRequest {
  nuevoEstado: EstadoIncidencia;
  notasDeveloper?: string;
}

export interface AddComentarioRequest {
  texto: string;
}
