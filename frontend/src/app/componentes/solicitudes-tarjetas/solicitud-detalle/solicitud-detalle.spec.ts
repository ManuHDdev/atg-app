import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { SolicitudDetalle } from './solicitud-detalle';
import { SolicitudTarjetaService } from '../../../services/solicitud-tarjeta.service';
import { SocioService } from '../../../services/socio.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { SolicitudTarjeta } from '../../../models/solicitud-tarjeta.model';

/**
 * Lo que se comprueba aquí son las puertas del circuito del documento firmado: qué botón
 * puede aparecer en cada estado. Es la parte de la pantalla donde un error deja al operador
 * presentando a la petrolera una solicitud sin firma.
 */
describe('SolicitudDetalle', () => {
  let solicitudServiceSpy: jasmine.SpyObj<SolicitudTarjetaService>;

  const SOLICITUD_BASE: SolicitudTarjeta = {
    id: '1',
    numeroSolicitud: 'TAR-2026-00001',
    socioId: '10',
    petroleraId: '20',
    matricula: '1234ABC',
    tipo: 'ALTA',
    estado: 'BORRADOR'
  };

  async function crearComponente(solicitud: SolicitudTarjeta): Promise<ComponentFixture<SolicitudDetalle>> {
    solicitudServiceSpy.getById.and.returnValue(of(solicitud));

    await TestBed.configureTestingModule({
      imports: [SolicitudDetalle],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: SolicitudTarjetaService, useValue: solicitudServiceSpy },
        { provide: SocioService, useValue: jasmine.createSpyObj('SocioService', { getById: of({} as any) }) },
        { provide: PetroleraService, useValue: jasmine.createSpyObj('PetroleraService', { getById: of({} as any) }) },
        { provide: ErrorHandlerService, useValue: jasmine.createSpyObj('ErrorHandlerService', { getMensaje: 'error de prueba' }) },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (_: string) => '1' } } }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(SolicitudDetalle);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
    solicitudServiceSpy = jasmine.createSpyObj('SolicitudTarjetaService', [
      'getById', 'guardarPdfEditado', 'enviarASocio', 'subirPdfFirmado',
      'aceptarFirmaSocio', 'enviarAPetrolera', 'descargarPdf'
    ]);
  });

  it('en borrador permite editar el impreso, pero no enviarlo hasta que exista', async () => {
    const sinImpreso = await crearComponente({ ...SOLICITUD_BASE });
    expect(sinImpreso.componentInstance.puedeEditarImpreso).toBeTrue();
    expect(sinImpreso.componentInstance.puedeEnviarASocio).toBeFalse();

    TestBed.resetTestingModule();
    const conImpreso = await crearComponente({ ...SOLICITUD_BASE, rutaPdfEditable: '/tmp/editable.pdf' });
    expect(conImpreso.componentInstance.puedeEnviarASocio).toBeTrue();
  });

  it('enviada al socio admite el escaneado firmado, y solo entonces aceptar la firma', async () => {
    const sinFirma = await crearComponente({ ...SOLICITUD_BASE, estado: 'ENVIADO_SOCIO' });
    expect(sinFirma.componentInstance.puedeSubirFirmado).toBeTrue();
    expect(sinFirma.componentInstance.puedeAceptarFirma).toBeFalse();
    expect(sinFirma.componentInstance.puedeEnviarAPetrolera).toBeFalse();

    TestBed.resetTestingModule();
    const conFirma = await crearComponente({
      ...SOLICITUD_BASE, estado: 'ENVIADO_SOCIO', rutaPdfFirmado: '/tmp/firmado.pdf'
    });
    expect(conFirma.componentInstance.puedeAceptarFirma).toBeTrue();
  });

  it('solo se presenta a la petrolera con la firma ya aceptada', async () => {
    const fixture = await crearComponente({ ...SOLICITUD_BASE, estado: 'FIRMADO_SOCIO' });
    const component = fixture.componentInstance;

    expect(component.puedeEnviarAPetrolera).toBeTrue();
    expect(component.puedeEditarImpreso).toBeFalse();
    expect(component.puedeSubirFirmado).toBeFalse();
  });

  it('presentada a la petrolera ya no deja tocar el documento', async () => {
    const fixture = await crearComponente({ ...SOLICITUD_BASE, estado: 'PENDIENTE' });
    const component = fixture.componentInstance;

    expect(component.puedeEditarImpreso).toBeFalse();
    expect(component.puedeSubirFirmado).toBeFalse();
    expect(component.puedeEnviarAPetrolera).toBeFalse();
    // A partir de aquí lo único que queda es registrar la respuesta de la petrolera.
    expect(component.puedeRegistrarRespuestaPetrolera).toBeTrue();
  });

  it('una LLEGADA no entra en el circuito del documento firmado', async () => {
    const fixture = await crearComponente({
      ...SOLICITUD_BASE, tipo: 'LLEGADA', estado: 'TARJETA_LLEGADA'
    });
    const component = fixture.componentInstance;

    expect(component.tieneCircuitoDeFirma).toBeFalse();
    expect(component.puedeEditarImpreso).toBeFalse();
    expect(component.puedeSubirFirmado).toBeFalse();
    expect(component.puedeEnviarAPetrolera).toBeFalse();
    expect(component.puedeMarcarEntregada).toBeTrue();
  });

  /** Las solicitudes anteriores al circuito no tienen número, y sin número no hay documento. */
  it('una solicitud sin número no muestra el bloque del documento', async () => {
    const fixture = await crearComponente({ ...SOLICITUD_BASE, numeroSolicitud: undefined });

    expect(fixture.componentInstance.tieneCircuitoDeFirma).toBeFalse();
    expect(fixture.componentInstance.puedeEditarImpreso).toBeFalse();
  });

  it('no envía a la petrolera si el operador cancela la confirmación', async () => {
    const fixture = await crearComponente({ ...SOLICITUD_BASE, estado: 'FIRMADO_SOCIO' });
    spyOn(window, 'confirm').and.returnValue(false);

    fixture.componentInstance.enviarAPetrolera();

    expect(solicitudServiceSpy.enviarAPetrolera).not.toHaveBeenCalled();
  });

  /** El tipo y el tamaño los valida la zona de subida; aquí solo se muestra el motivo. */
  it('muestra el motivo cuando la zona de subida rechaza un fichero', async () => {
    const fixture = await crearComponente({ ...SOLICITUD_BASE, estado: 'ENVIADO_SOCIO' });
    const component = fixture.componentInstance;

    component.onFicheroRechazado('Solo se admiten archivos PDF.');

    expect(component.ficheroFirmado).toBeNull();
    expect(component.error).toBe('Solo se admiten archivos PDF.');
  });

  /** Cada zona de subida tiene su propio fichero: elegir uno no deja el otro listo. */
  it('mantiene separados el impreso en borrador y el escaneado firmado', async () => {
    const fixture = await crearComponente({ ...SOLICITUD_BASE });
    const component = fixture.componentInstance;

    const pdf = new File([''], 'impreso.pdf', { type: 'application/pdf' });
    component.onFicheroEditableSeleccionado(pdf);

    expect(component.ficheroEditable).toBe(pdf);
    expect(component.ficheroFirmado).toBeNull();
  });
});
