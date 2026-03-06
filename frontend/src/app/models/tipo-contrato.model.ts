export interface TipoContrato {
  id?: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  activo: boolean;
  fechaCreacion?: Date;
  fechaActualizacion?: Date;
}
