export interface Tarjeta {
  id?: string;
  socioId: string;
  petroleraId: string;
  matricula: string;
  numeroContrato: string;
  fechaAlta?: Date;
  fechaBaja?: string;  // ISO date string
  cantidad: number;  // Número de tarjetas físicas (se incrementa con duplicados)
  activa: boolean;
}
