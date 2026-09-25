import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { ContratoDetail } from './contrato-detail';
import {
  EstadoSolicitud,
  SolicitudContrato,
  TipoSolicitudContrato
} from '../../../models/solicitud-contrato.model';

/**
 * ATG no aprueba ni deniega nada: solo registra lo que ha decidido la petrolera. Esta pantalla
 * usaba "Aceptar/Rechazar Solicitud", que hacía parecer lo contrario; el vocabulario tiene que
 * ser el mismo que en tarjetas ("Registrar aprobación/denegación de la petrolera").
 */
describe('ContratoDetail — vocabulario de la resolución de la petrolera', () => {
  let fixture: ComponentFixture<ContratoDetail>;
  let component: ContratoDetail;

  function solicitudEn(estado: EstadoSolicitud): SolicitudContrato {
    return {
      id: 1,
      numeroSolicitud: 'SOL-2026-00001',
      socioId: 1,
      petroleraId: 2,
      tipoContratoId: 3,
      esAutonomo: false,
      tipoSolicitud: TipoSolicitudContrato.NUEVO,
      estado,
      fechaEnvioPetrolera: new Date('2026-01-15T10:00:00')
    };
  }

  function pintar(estado: EstadoSolicitud): string {
    component.solicitud = solicitudEn(estado);
    fixture.detectChanges();
    return fixture.nativeElement.textContent ?? '';
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContratoDetail],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (_: string) => null } } }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContratoDetail);
    component = fixture.componentInstance;
  });

  it('pide registrar la decisión de la petrolera en vez de aceptar o rechazar', () => {
    const texto = pintar(EstadoSolicitud.ENVIADO_PETROLERA);

    expect(texto).toContain('Registrar aprobación de la petrolera');
    expect(texto).toContain('Registrar denegación de la petrolera');
    expect(texto).not.toContain('Aceptar Solicitud');
    expect(texto).not.toContain('Rechazar Solicitud');
  });

  it('atribuye la resolución a la petrolera cuando ya está registrada', () => {
    expect(pintar(EstadoSolicitud.ACEPTADA_PETROLERA)).toContain('La petrolera ha');
    expect(pintar(EstadoSolicitud.RECHAZADA_PETROLERA)).toContain('denegado');
  });

  it('etiqueta los estados con el mismo vocabulario que tarjetas', () => {
    expect(component.getEstadoTexto(EstadoSolicitud.ACEPTADA_PETROLERA)).toBe(
      'Aprobada por la petrolera'
    );
    expect(component.getEstadoTexto(EstadoSolicitud.RECHAZADA_PETROLERA)).toBe(
      'Denegada por la petrolera'
    );
  });
});
