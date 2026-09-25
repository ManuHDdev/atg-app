import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { TiposSolicitud } from './tipos-solicitud';
import { TipoSolicitudService } from '../../../services/tipo-solicitud.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { TipoSolicitud } from '../../../models/tipo-solicitud.model';
import { Petrolera } from '../../../models/petrolera.model';

describe('TiposSolicitud', () => {
  const PETROLERAS: Petrolera[] = [
    { id: 7, nombre: 'Repsol', activa: true },
    { id: 9, nombre: 'Cepsa', activa: true },
    { id: 11, nombre: 'Petrolera de Baja', activa: false }
  ];

  const TIPOS_DE_REPSOL: TipoSolicitud[] = [
    { id: '1', nombre: 'Precio Lista', codigo: 'PRECIO_LISTA', activa: true }
  ];

  let tipoSolicitudServiceSpy: jasmine.SpyObj<TipoSolicitudService>;
  let petroleraServiceSpy: jasmine.SpyObj<PetroleraService>;

  /**
   * Monta la pantalla tal y como se llega a ella: con `petroleraIdRuta` cuando se entra desde el
   * enlace "Ver" de petroleras (?petroleraId=N) y con null cuando se entra desde el menú lateral.
   */
  async function crearComponente(
    petroleraIdRuta: string | null
  ): Promise<ComponentFixture<TiposSolicitud>> {
    await TestBed.configureTestingModule({
      imports: [TiposSolicitud],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: TipoSolicitudService, useValue: tipoSolicitudServiceSpy },
        { provide: PetroleraService, useValue: petroleraServiceSpy },
        {
          provide: NotificationService,
          useValue: jasmine.createSpyObj('NotificationService', ['success', 'error'])
        },
        {
          provide: ErrorHandlerService,
          useValue: jasmine.createSpyObj('ErrorHandlerService', { getMensaje: 'error de prueba' })
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: (_: string) => petroleraIdRuta } } }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(TiposSolicitud);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  function selector(fixture: ComponentFixture<TiposSolicitud>): HTMLSelectElement | null {
    return fixture.nativeElement.querySelector('#selectorPetrolera');
  }

  function textoDeLaPantalla(fixture: ComponentFixture<TiposSolicitud>): string {
    return fixture.nativeElement.textContent ?? '';
  }

  beforeEach(() => {
    tipoSolicitudServiceSpy = jasmine.createSpyObj('TipoSolicitudService', [
      'getAll',
      'getByPetroleraId'
    ]);
    tipoSolicitudServiceSpy.getAll.and.returnValue(of([]));
    tipoSolicitudServiceSpy.getByPetroleraId.and.returnValue(of(TIPOS_DE_REPSOL));

    petroleraServiceSpy = jasmine.createSpyObj('PetroleraService', ['getAll', 'getById']);
    petroleraServiceSpy.getAll.and.returnValue(of(PETROLERAS));
    petroleraServiceSpy.getById.and.returnValue(of(PETROLERAS[0]));
  });

  describe('sin ?petroleraId= (entrada desde el menú)', () => {
    it('ofrece un selector con las petroleras activas en lugar de mandar a otra pantalla', async () => {
      const fixture = await crearComponente(null);

      const select = selector(fixture);
      expect(select).not.toBeNull();

      const opciones = Array.from(select!.querySelectorAll('option')).map(o => o.textContent?.trim());
      expect(opciones).toContain('Repsol');
      expect(opciones).toContain('Cepsa');
      // El listado filtra por activa = true, igual que el resto de pantallas.
      expect(opciones).not.toContain('Petrolera de Baja');

      expect(tipoSolicitudServiceSpy.getByPetroleraId).not.toHaveBeenCalled();
      expect(textoDeLaPantalla(fixture)).toContain('Todavía no hay ninguna petrolera seleccionada');
    });

    it('carga los tipos de la petrolera elegida en el selector', async () => {
      const fixture = await crearComponente(null);
      const select = selector(fixture)!;

      select.value = '7';
      select.dispatchEvent(new Event('change'));
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(tipoSolicitudServiceSpy.getByPetroleraId).toHaveBeenCalledWith('7');
      expect(fixture.componentInstance.petroleraId).toBe('7');

      const texto = textoDeLaPantalla(fixture);
      expect(texto).toContain('Tipos de Solicitud de Repsol');
      expect(texto).toContain('Precio Lista');
      expect(texto).toContain('+ Nuevo Tipo de Solicitud');
    });

    it('vuelve al estado inicial si se deselecciona la petrolera', async () => {
      const fixture = await crearComponente(null);
      const select = selector(fixture)!;

      select.value = '7';
      select.dispatchEvent(new Event('change'));
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      select.value = '';
      select.dispatchEvent(new Event('change'));
      fixture.detectChanges();

      expect(fixture.componentInstance.petroleraId).toBeNull();
      expect(fixture.componentInstance.tiposSolicitud).toEqual([]);
      expect(textoDeLaPantalla(fixture)).toContain('Todavía no hay ninguna petrolera seleccionada');
    });
  });

  describe('con ?petroleraId= (entrada desde el enlace "Ver" de petroleras)', () => {
    it('carga directamente esa petrolera y no muestra el selector', async () => {
      const fixture = await crearComponente('7');

      expect(tipoSolicitudServiceSpy.getByPetroleraId).toHaveBeenCalledWith('7');
      expect(fixture.componentInstance.petroleraId).toBe('7');
      expect(selector(fixture)).toBeNull();

      const texto = textoDeLaPantalla(fixture);
      expect(texto).toContain('Tipos de Solicitud de Repsol');
      expect(texto).toContain('Precio Lista');
      expect(texto).toContain('Volver a Petroleras');
    });
  });
});
