export interface Socio {
  id?: string;
  nombre: string;
  nif: string;
  direccion: string;
  poblacion: string;
  provincia: string;
  codigoPostal: string;
  email: string;
  telefono: string;
  agrupacion: 'ATG' | 'ATT';
  numeroSocio: string;
  esAutonomo: boolean;
  fechaAlta?: Date;
  activo: boolean;
}
