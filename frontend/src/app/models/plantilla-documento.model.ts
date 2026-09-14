/**
 * Plantilla PDF que la ATG envía al socio para que la firme, por petrolera y módulo.
 * Almacén independiente del de contratos: no depende de TipoSolicitud ni de TipoContrato.
 */
export enum ModuloDocumento {
  TARJETAS = 'TARJETAS',
  DISPOSITIVOS = 'DISPOSITIVOS'
}

export interface PlantillaDocumento {
  id?: number;
  petroleraId: number;
  petroleraNombre?: string;
  modulo: ModuloDocumento;
  tipoSolicitud: string;
  nombreArchivo?: string;
  rutaArchivo?: string;
  activa: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface OpcionModulo {
  value: ModuloDocumento;
  label: string;
}

export const MODULOS_DOCUMENTO: OpcionModulo[] = [
  { value: ModuloDocumento.TARJETAS, label: 'Tarjetas' },
  { value: ModuloDocumento.DISPOSITIVOS, label: 'Dispositivos' }
];

/**
 * Tipos de solicitud por módulo. Son los nombres de las constantes del enum
 * TipoSolicitud de cada microservicio consumidor, enviados como texto.
 */
export const TIPOS_SOLICITUD_POR_MODULO: Record<ModuloDocumento, { value: string; label: string }[]> = {
  [ModuloDocumento.TARJETAS]: [
    { value: 'ALTA', label: 'Alta de tarjeta' },
    { value: 'BAJA', label: 'Baja de tarjeta' },
    { value: 'DUPLICADO', label: 'Duplicado de tarjeta' }
  ],
  [ModuloDocumento.DISPOSITIVOS]: [
    { value: 'ALTA_DISPOSITIVO', label: 'Alta de dispositivo' },
    { value: 'SOLICITUD_CREDITO', label: 'Solicitud de crédito' },
    { value: 'BAJA_DISPOSITIVO', label: 'Baja de dispositivo' },
    { value: 'CAMBIO_MATRICULA', label: 'Cambio de matrícula' }
  ]
};

export function getModuloLabel(modulo: ModuloDocumento): string {
  return MODULOS_DOCUMENTO.find(m => m.value === modulo)?.label ?? modulo;
}

export function getTipoSolicitudLabel(modulo: ModuloDocumento, tipoSolicitud: string): string {
  const tipos = TIPOS_SOLICITUD_POR_MODULO[modulo] ?? [];
  return tipos.find(t => t.value === tipoSolicitud)?.label ?? tipoSolicitud;
}
