export interface PlantillaTarjeta {
  id?: string;
  tipo: 'LLEGADA_MADRID' | 'LLEGADA_FUERA' | 'ALTA_SOCIO' | 'ALTA_PETROLERA' | 'BAJA_SOCIO' | 'DUPLICADO_SOCIO';
  asunto: string;
  cuerpo: string;
  variablesDisponibles?: string;
  activa: boolean;
  createdAt?: Date;
  updatedAt?: Date;
}

export const VARIABLES_DISPONIBLES = [
  { variable: '{nombre}', descripcion: 'Nombre del socio' },
  { variable: '{nif}', descripcion: 'NIF/Número de socio' },
  { variable: '{email}', descripcion: 'Email del socio' },
  { variable: '{telefono}', descripcion: 'Teléfono del socio' },
  { variable: '{provincia}', descripcion: 'Provincia del socio' },
  { variable: '{matricula}', descripcion: 'Matrícula del vehículo' },
  { variable: '{numeroContrato}', descripcion: 'Número de contrato' },
  { variable: '{fecha}', descripcion: 'Fecha actual' },
  { variable: '{nombrePetrolera}', descripcion: 'Nombre de la petrolera' },
  { variable: '{emailPetrolera}', descripcion: 'Email de la petrolera' }
];
