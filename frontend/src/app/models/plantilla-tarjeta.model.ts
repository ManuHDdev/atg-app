export type TipoPlantillaTarjeta =
  | 'LLEGADA_MADRID'
  | 'LLEGADA_FUERA'
  | 'ALTA_SOCIO'
  | 'ALTA_PETROLERA'
  | 'ALTA_APROBADA'
  | 'ALTA_RECHAZADA'
  | 'BAJA_SOCIO'
  | 'BAJA_CONFIRMADA'
  | 'DUPLICADO_SOCIO'
  | 'DUPLICADO_CONFIRMADA'
  | 'DUPLICADO_PETROLERA';

export interface PlantillaTarjeta {
  id?: string;
  tipo: TipoPlantillaTarjeta;
  asunto: string;
  cuerpo: string;
  variablesDisponibles?: string;
  activa: boolean;
  createdAt?: Date;
  updatedAt?: Date;
}

export const TIPOS_PLANTILLA_TARJETA: TipoPlantillaTarjeta[] = [
  'LLEGADA_MADRID',
  'LLEGADA_FUERA',
  'ALTA_SOCIO',
  'ALTA_PETROLERA',
  'ALTA_APROBADA',
  'ALTA_RECHAZADA',
  'BAJA_SOCIO',
  'BAJA_CONFIRMADA',
  'DUPLICADO_SOCIO',
  'DUPLICADO_CONFIRMADA',
  'DUPLICADO_PETROLERA'
];

export const TIPO_PLANTILLA_LABELS: Record<TipoPlantillaTarjeta, string> = {
  'LLEGADA_MADRID': 'Llegada - Madrid',
  'LLEGADA_FUERA': 'Llegada - Otras Provincias',
  'ALTA_SOCIO': 'Alta - Correo al Socio',
  'ALTA_PETROLERA': 'Alta - Correo a Petrolera',
  'ALTA_APROBADA': 'Alta - Aprobada por la Petrolera',
  'ALTA_RECHAZADA': 'Alta - Rechazada por la Petrolera',
  'BAJA_SOCIO': 'Baja - Correo al Socio',
  'BAJA_CONFIRMADA': 'Baja - Confirmada por la Petrolera',
  'DUPLICADO_SOCIO': 'Duplicado - Correo al Socio',
  'DUPLICADO_CONFIRMADA': 'Duplicado - Confirmado por la Petrolera',
  'DUPLICADO_PETROLERA': 'Duplicado - Correo a Petrolera'
};

export function getTipoPlantillaLabel(tipo: string): string {
  return TIPO_PLANTILLA_LABELS[tipo as TipoPlantillaTarjeta] ?? tipo;
}

export const VARIABLES_DISPONIBLES = [
  { variable: '{nombre}', descripcion: 'Nombre del socio' },
  { variable: '{nif}', descripcion: 'NIF/Número de socio' },
  { variable: '{email}', descripcion: 'Email del socio' },
  { variable: '{telefono}', descripcion: 'Teléfono del socio' },
  { variable: '{direccion}', descripcion: 'Dirección del socio' },
  { variable: '{poblacion}', descripcion: 'Población del socio' },
  { variable: '{codigoPostal}', descripcion: 'Código postal del socio' },
  { variable: '{provincia}', descripcion: 'Provincia del socio' },
  { variable: '{direccionCompleta}', descripcion: 'Dirección postal completa del socio' },
  { variable: '{matricula}', descripcion: 'Matrícula del vehículo' },
  { variable: '{numeroContrato}', descripcion: 'Número de contrato' },
  { variable: '{fecha}', descripcion: 'Fecha actual' },
  { variable: '{nombrePetrolera}', descripcion: 'Nombre de la petrolera' },
  { variable: '{emailPetrolera}', descripcion: 'Email de la petrolera' },
  { variable: '{motivo}', descripcion: 'Motivo del rechazo (solo Alta rechazada)' }
];
