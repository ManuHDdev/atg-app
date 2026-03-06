export interface Empresa {
  id?: string;
  socioId?: string;
  socioNombre?: string;
  nombre: string;
  razonSocial: string;
  cif: string;
  direccion: string;
  poblacion: string;
  provincia: string;
  codigoPostal: string;
  email: string;
  telefono: string;
  fechaAlta?: Date;
  activa: boolean;
  createdAt?: Date;
  updatedAt?: Date;
}
