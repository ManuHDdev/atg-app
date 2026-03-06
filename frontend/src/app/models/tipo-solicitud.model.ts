export interface TipoSolicitud {
  id?: string;
  petroleraId?: string;
  petroleraNombre?: string;
  nombre: string;
  codigo: string;
  orden: number;
  activa: boolean;
  rutaPlantillaPdf?: string;
  nombreArchivoPlantilla?: string;
  createdAt?: Date;
  updatedAt?: Date;
}
