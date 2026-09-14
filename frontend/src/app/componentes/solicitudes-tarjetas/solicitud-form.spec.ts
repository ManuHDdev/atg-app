import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { SolicitudForm } from './solicitud-form';
import { SolicitudTarjetaService } from '../../services/solicitud-tarjeta.service';
import { SocioService } from '../../services/socio.service';
import { PetroleraService } from '../../services/petrolera.service';
import { TarjetaService } from '../../services/tarjeta.service';
import { ErrorHandlerService } from '../../services/error-handler.service';

describe('SolicitudForm', () => {
  let solicitudServiceSpy: jasmine.SpyObj<SolicitudTarjetaService>;

  /** Monta el formulario para el tipo de solicitud indicado (el que llega por la ruta). */
  async function crearComponente(tipo: string): Promise<ComponentFixture<SolicitudForm>> {
    await TestBed.configureTestingModule({
      imports: [SolicitudForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: SolicitudTarjetaService, useValue: solicitudServiceSpy },
        { provide: SocioService, useValue: jasmine.createSpyObj('SocioService', { getAll: of([]) }) },
        { provide: PetroleraService, useValue: jasmine.createSpyObj('PetroleraService', { getAll: of([]) }) },
        { provide: TarjetaService, useValue: jasmine.createSpyObj('TarjetaService', { getAll: of([]) }) },
        { provide: ErrorHandlerService, useValue: jasmine.createSpyObj('ErrorHandlerService', { getMensaje: 'error de prueba' }) },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (_: string) => tipo } } }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(SolicitudForm);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    solicitudServiceSpy = jasmine.createSpyObj('SolicitudTarjetaService', ['create']);
    solicitudServiceSpy.create.and.returnValue(of({} as any));
  });

  describe('motivo del duplicado', () => {
    it('es obligatorio y bloquea el envío mientras falte', async () => {
      const fixture = await crearComponente('duplicado');
      const component = fixture.componentInstance;

      component.formulario.patchValue({
        socioId: '10',
        petroleraId: '20',
        matricula: '1234ABC',
        tarjetaId: '30'
      });

      expect(component.formulario.valid).toBeFalse();

      component.onSubmit();

      expect(component.intentoGuardar).toBeTrue();
      expect(component.campoInvalido('motivoDuplicado')).toBeTrue();
      expect(solicitudServiceSpy.create).not.toHaveBeenCalled();
    });

    it('se envía al backend cuando se elige', async () => {
      const fixture = await crearComponente('duplicado');
      const component = fixture.componentInstance;

      component.formulario.patchValue({
        socioId: '10',
        petroleraId: '20',
        matricula: '1234ABC',
        tarjetaId: '30',
        motivoDuplicado: 'EXTRAVIO'
      });

      component.onSubmit();

      expect(component.campoInvalido('motivoDuplicado')).toBeFalse();
      expect(solicitudServiceSpy.create).toHaveBeenCalledWith(
        jasmine.objectContaining({ tipo: 'DUPLICADO', motivoDuplicado: 'EXTRAVIO' })
      );
    });

    it('no existe como campo en los demás tipos de solicitud', async () => {
      const fixture = await crearComponente('alta');
      const component = fixture.componentInstance;

      expect(component.formulario.get('motivoDuplicado')).toBeNull();
    });
  });
});
