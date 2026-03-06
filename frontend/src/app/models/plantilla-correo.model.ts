export interface PlantillaCorreo {
  id?: number;
  petroleraId: number;
  petroleraNombre?: string;
  tipoPlantilla: string;
  asunto: string;
  cuerpo: string;
  variablesDisponibles?: string;
  activa: boolean;
  createdAt?: string;
  updatedAt?: string;
}
