export interface Petrolera {
  id?: number;
  nombre: string;
  activa: boolean;
  email?: string;
  diasEnvioCreditos?: string;
  // Disponibilidad por módulo. null/undefined = sin restricción configurada => permitido.
  operaTarjetas?: boolean | null;
  operaContratos?: boolean | null;
  operaCreditos?: boolean | null;
  operaDispositivos?: boolean | null;
  permiteCreditoDispositivo?: boolean | null;
}

/**
 * Regla única de interpretación de los flags de disponibilidad de una petrolera:
 * un flag sin valor (null/undefined) significa "sin restricción configurada" y por tanto
 * se permite. Solo bloquea cuando el administrador lo ha desmarcado explícitamente.
 */
export function petroleraPermite(flag: boolean | null | undefined): boolean {
  return flag === null || flag === undefined || flag === true;
}
