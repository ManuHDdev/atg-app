import { TipoEventoEmail, TIPOS_EVENTO_EMAIL, VARIABLES_POR_TIPO } from './plantilla-email.model';

/**
 * Tipos de plantilla que el backend de dispositivos busca al enviar los correos del circuito
 * del documento firmado (SolicitudDispositivoService: PLANTILLA_DOCUMENTO_SOCIO y
 * PLANTILLA_DOCUMENTO_PETROLERA). Si no se pueden crear desde la pantalla, esos correos
 * salen con el texto por defecto o directamente no salen.
 */
const TIPOS_DOCUMENTO_DISPOSITIVO_BACKEND = [
  'DOCUMENTO_SOCIO_DISPOSITIVO',
  'DOCUMENTO_PETROLERA_DISPOSITIVO'
];

describe('plantilla-email.model', () => {
  it('el desplegable ofrece un tipo por cada valor del enum', () => {
    expect(TIPOS_EVENTO_EMAIL.map(t => t.value)).toEqual(Object.values(TipoEventoEmail));
    TIPOS_EVENTO_EMAIL.forEach(tipo => expect(tipo.label).toBeTruthy());
  });

  it('ofrece los tipos del circuito del documento firmado de dispositivos', () => {
    const ofrecidos = TIPOS_EVENTO_EMAIL.map(t => String(t.value));
    TIPOS_DOCUMENTO_DISPOSITIVO_BACKEND.forEach(tipo => expect(ofrecidos).toContain(tipo));
  });

  it('cada tipo declara sus variables disponibles', () => {
    Object.values(TipoEventoEmail).forEach(tipo => {
      expect(VARIABLES_POR_TIPO[tipo]).withContext(tipo).toBeTruthy();
      expect(VARIABLES_POR_TIPO[tipo].length).toBeGreaterThan(0);
    });
  });
});
