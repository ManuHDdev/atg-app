import {
  TIPOS_PLANTILLA_TARJETA,
  getTipoPlantillaLabel,
  getTipoPlantillaIcono
} from './plantilla-tarjeta.model';

/**
 * Copia literal del enum `TipoPlantilla` del backend
 * (backend/tarjetas/src/main/java/com/manuhd/app/tarjetas/model/TipoPlantilla.java).
 *
 * Si se añade un tipo allí y no aquí, este spec falla: es la red que evita que la pantalla
 * de plantillas se quede sin ofrecer un tipo que el backend sí busca al enviar el correo
 * (fue exactamente lo que dejó a DOCUMENTO_SOCIO y DOCUMENTO_PETROLERA sin poder crearse).
 */
const TIPOS_PLANTILLA_BACKEND = [
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
  'DUPLICADO_PETROLERA',
  'DOCUMENTO_SOCIO',
  'DOCUMENTO_PETROLERA'
];

describe('plantilla-tarjeta.model', () => {
  it('ofrece exactamente los tipos que declara el backend', () => {
    const ofrecidos: string[] = [...TIPOS_PLANTILLA_TARJETA];

    expect(ofrecidos.sort()).toEqual([...TIPOS_PLANTILLA_BACKEND].sort());
  });

  it('incluye los dos tipos del circuito del documento firmado', () => {
    expect(TIPOS_PLANTILLA_TARJETA).toContain('DOCUMENTO_SOCIO');
    expect(TIPOS_PLANTILLA_TARJETA).toContain('DOCUMENTO_PETROLERA');
  });

  it('cada tipo tiene etiqueta e icono propios', () => {
    const etiquetas = TIPOS_PLANTILLA_TARJETA.map(tipo => getTipoPlantillaLabel(tipo));

    TIPOS_PLANTILLA_TARJETA.forEach(tipo => {
      // Si faltara la etiqueta, getTipoPlantillaLabel devolvería el propio código del tipo.
      expect(getTipoPlantillaLabel(tipo)).not.toBe(tipo);
      expect(getTipoPlantillaIcono(tipo)).toMatch(/^bi-/);
    });
    expect(new Set(etiquetas).size).toBe(TIPOS_PLANTILLA_TARJETA.length);
  });

  it('devuelve el propio código y un icono genérico para un tipo desconocido', () => {
    expect(getTipoPlantillaLabel('TIPO_INVENTADO')).toBe('TIPO_INVENTADO');
    expect(getTipoPlantillaIcono('TIPO_INVENTADO')).toBe('bi-envelope-fill');
  });
});
