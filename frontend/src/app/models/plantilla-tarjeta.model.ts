/**
 * Catálogo único de tipos de plantilla de correo de tarjetas.
 *
 * ESTE OBJETO ES LA ÚNICA LISTA: el tipo, el listado del desplegable, las etiquetas y los
 * iconos se derivan de aquí. Debe contener exactamente los mismos valores que el enum
 * `TipoPlantilla` del backend (backend/tarjetas/.../model/TipoPlantilla.java): si el backend
 * declara un tipo que no está aquí, esa plantilla no se puede crear desde la pantalla y el
 * correo correspondiente no se envía nunca. `plantilla-tarjeta.model.spec.ts` compara ambas
 * listas y falla si dejan de coincidir.
 */
const PLANTILLAS_TARJETA = {
  LLEGADA_MADRID: { label: 'Llegada - Madrid', icono: 'bi-box-seam' },
  LLEGADA_FUERA: { label: 'Llegada - Otras Provincias', icono: 'bi-mailbox' },
  ALTA_SOCIO: { label: 'Alta - Correo al Socio', icono: 'bi-envelope' },
  ALTA_PETROLERA: { label: 'Alta - Correo a Petrolera', icono: 'bi-envelope-fill' },
  ALTA_APROBADA: { label: 'Alta - Aprobada por la Petrolera', icono: 'bi-check-circle' },
  ALTA_RECHAZADA: { label: 'Alta - Rechazada por la Petrolera', icono: 'bi-x-circle' },
  BAJA_SOCIO: { label: 'Baja - Correo al Socio', icono: 'bi-envelope-open' },
  BAJA_CONFIRMADA: { label: 'Baja - Confirmada por la Petrolera', icono: 'bi-check2-square' },
  DUPLICADO_SOCIO: { label: 'Duplicado - Correo al Socio', icono: 'bi-file-earmark' },
  DUPLICADO_CONFIRMADA: { label: 'Duplicado - Confirmado por la Petrolera', icono: 'bi-file-earmark-check' },
  DUPLICADO_PETROLERA: { label: 'Duplicado - Correo a Petrolera', icono: 'bi-file-earmark-arrow-up' },
  DOCUMENTO_SOCIO: { label: 'Documento - Envío al Socio para Firma', icono: 'bi-pen' },
  DOCUMENTO_PETROLERA: { label: 'Documento - Envío del Firmado a la Petrolera', icono: 'bi-send-check' }
} as const satisfies Record<string, { label: string; icono: string }>;

export type TipoPlantillaTarjeta = keyof typeof PLANTILLAS_TARJETA;

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

/** Tipos que ofrece el desplegable de creación de plantillas, en el orden del catálogo. */
export const TIPOS_PLANTILLA_TARJETA = Object.keys(PLANTILLAS_TARJETA) as TipoPlantillaTarjeta[];

export const TIPO_PLANTILLA_LABELS = Object.fromEntries(
  Object.entries(PLANTILLAS_TARJETA).map(([tipo, meta]) => [tipo, meta.label])
) as Record<TipoPlantillaTarjeta, string>;

export function getTipoPlantillaLabel(tipo: string): string {
  return TIPO_PLANTILLA_LABELS[tipo as TipoPlantillaTarjeta] ?? tipo;
}

/** Icono de Bootstrap Icons con el que se pinta cada tipo en el listado. */
export function getTipoPlantillaIcono(tipo: string): string {
  return PLANTILLAS_TARJETA[tipo as TipoPlantillaTarjeta]?.icono ?? 'bi-envelope-fill';
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
  { variable: '{motivo}', descripcion: 'Motivo del rechazo (solo Alta rechazada)' },
  { variable: '{motivoDuplicado}', descripcion: 'Motivo del duplicado: Deterioro o Extravío (solo Duplicado)' }
];
