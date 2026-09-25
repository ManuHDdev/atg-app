import {
  TIPOS_PLANTILLA_TARJETA,
  TIPOS_PLANTILLA_DECLARADOS,
  TIPOS_PLANTILLA_OBSOLETOS,
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

/**
 * Tipos que el backend conserva solo para poder leer las plantillas ya guardadas: no los
 * envía ningún flujo, así que la pantalla no debe ofrecerlos al crear una plantilla nueva.
 * Para retirar un tipo hay que añadirlo aquí a conciencia; un tipo nuevo del backend que
 * no esté en esta lista y tampoco en el desplegable sigue haciendo fallar el spec.
 */
const TIPOS_PLANTILLA_RETIRADOS = ['ALTA_PETROLERA', 'DUPLICADO_PETROLERA'];

describe('plantilla-tarjeta.model', () => {
  it('conoce exactamente los tipos que declara el backend', () => {
    const declarados: string[] = [...TIPOS_PLANTILLA_DECLARADOS];

    expect(declarados.sort()).toEqual([...TIPOS_PLANTILLA_BACKEND].sort());
  });

  it('ofrece todos los tipos declarados salvo los retirados', () => {
    const ofrecidos: string[] = [...TIPOS_PLANTILLA_TARJETA];
    const esperados = TIPOS_PLANTILLA_BACKEND.filter(
      tipo => !TIPOS_PLANTILLA_RETIRADOS.includes(tipo)
    );

    expect(ofrecidos.sort()).toEqual(esperados.sort());
  });

  it('marca como obsoletos exactamente los tipos retirados', () => {
    const obsoletos: string[] = [...TIPOS_PLANTILLA_OBSOLETOS];

    expect(obsoletos.sort()).toEqual([...TIPOS_PLANTILLA_RETIRADOS].sort());
  });

  it('no ofrece los correos a la petrolera que ya no se envían', () => {
    expect(TIPOS_PLANTILLA_TARJETA).not.toContain('ALTA_PETROLERA');
    expect(TIPOS_PLANTILLA_TARJETA).not.toContain('DUPLICADO_PETROLERA');
  });

  it('incluye los dos tipos del circuito del documento firmado', () => {
    expect(TIPOS_PLANTILLA_TARJETA).toContain('DOCUMENTO_SOCIO');
    expect(TIPOS_PLANTILLA_TARJETA).toContain('DOCUMENTO_PETROLERA');
  });

  it('cada tipo declarado tiene etiqueta e icono propios, también los retirados', () => {
    const etiquetas = TIPOS_PLANTILLA_DECLARADOS.map(tipo => getTipoPlantillaLabel(tipo));

    TIPOS_PLANTILLA_DECLARADOS.forEach(tipo => {
      // Si faltara la etiqueta, getTipoPlantillaLabel devolvería el propio código del tipo.
      expect(getTipoPlantillaLabel(tipo)).not.toBe(tipo);
      expect(getTipoPlantillaIcono(tipo)).toMatch(/^bi-/);
    });
    expect(new Set(etiquetas).size).toBe(TIPOS_PLANTILLA_DECLARADOS.length);
  });

  it('devuelve el propio código y un icono genérico para un tipo desconocido', () => {
    expect(getTipoPlantillaLabel('TIPO_INVENTADO')).toBe('TIPO_INVENTADO');
    expect(getTipoPlantillaIcono('TIPO_INVENTADO')).toBe('bi-envelope-fill');
  });
});
