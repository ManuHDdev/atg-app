import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PlantillaForm } from './plantilla-form';
import { PlantillaTarjetaService } from '../../services/plantilla-tarjeta.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { PlantillaTarjeta, TIPOS_PLANTILLA_TARJETA } from '../../models/plantilla-tarjeta.model';

describe('PlantillaForm', () => {
  let plantillaServiceSpy: jasmine.SpyObj<PlantillaTarjetaService>;

  const plantillaExistente: PlantillaTarjeta = {
    id: '1',
    tipo: 'ALTA_SOCIO',
    asunto: 'Asunto existente',
    cuerpo: '<p>Cuerpo existente</p>',
    activa: true
  };

  /** Configura el TestBed con el id de ruta indicado (null = modo creación). */
  async function crearComponente(id: string | null): Promise<ComponentFixture<PlantillaForm>> {
    await TestBed.configureTestingModule({
      imports: [PlantillaForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: PlantillaTarjetaService, useValue: plantillaServiceSpy },
        { provide: ErrorHandlerService, useValue: jasmine.createSpyObj('ErrorHandlerService', { getMensaje: 'error de prueba' }) },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: (_: string) => id } } }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(PlantillaForm);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    plantillaServiceSpy = jasmine.createSpyObj('PlantillaTarjetaService', ['getAll', 'getById', 'create', 'update']);
    plantillaServiceSpy.getAll.and.returnValue(of([]));
    plantillaServiceSpy.getById.and.returnValue(of(plantillaExistente));
    plantillaServiceSpy.create.and.returnValue(of(plantillaExistente));
    plantillaServiceSpy.update.and.returnValue(of(plantillaExistente));
  });

  describe('modo creación', () => {
    it('llama a create y no a update al guardar', async () => {
      const fixture = await crearComponente(null);
      const component = fixture.componentInstance;

      expect(component.modoCreacion).toBeTrue();

      component.formulario.patchValue({
        tipo: 'ALTA_APROBADA',
        asunto: 'Alta aprobada',
        cuerpo: '<p>Hola {nombre}</p>'
      });
      component.onSubmit();

      expect(plantillaServiceSpy.create).toHaveBeenCalled();
      expect(plantillaServiceSpy.update).not.toHaveBeenCalled();
      const enviada = plantillaServiceSpy.create.calls.mostRecent().args[0];
      expect(enviada.tipo).toBe('ALTA_APROBADA');
      expect(enviada.asunto).toBe('Alta aprobada');
    });

    it('el desplegable ofrece todos los tipos cuando no hay ninguna plantilla creada', async () => {
      const fixture = await crearComponente(null);
      const component = fixture.componentInstance;

      // Sin esto, un tipo que el backend sí busca al enviar el correo no se podría crear.
      expect(component.tiposDisponibles).toEqual(TIPOS_PLANTILLA_TARJETA);
      expect(component.tiposDisponibles).toContain('DOCUMENTO_SOCIO');
      expect(component.tiposDisponibles).toContain('DOCUMENTO_PETROLERA');
    });

    it('el desplegable de tipos excluye los tipos que ya tienen plantilla', async () => {
      plantillaServiceSpy.getAll.and.returnValue(of([
        { ...plantillaExistente, tipo: 'ALTA_SOCIO' },
        { ...plantillaExistente, id: '2', tipo: 'BAJA_SOCIO' }
      ]));

      const fixture = await crearComponente(null);
      const component = fixture.componentInstance;

      expect(component.tiposDisponibles).not.toContain('ALTA_SOCIO');
      expect(component.tiposDisponibles).not.toContain('BAJA_SOCIO');
      expect(component.tiposDisponibles).toContain('ALTA_APROBADA');
      expect(component.tiposDisponibles.length).toBe(TIPOS_PLANTILLA_TARJETA.length - 2);
      expect(component.sinTiposDisponibles).toBeFalse();
    });

    it('deshabilita el formulario cuando todos los tipos ya tienen plantilla', async () => {
      plantillaServiceSpy.getAll.and.returnValue(of(
        TIPOS_PLANTILLA_TARJETA.map((tipo, i) => ({ ...plantillaExistente, id: String(i), tipo }))
      ));

      const fixture = await crearComponente(null);
      const component = fixture.componentInstance;

      expect(component.tiposDisponibles.length).toBe(0);
      expect(component.sinTiposDisponibles).toBeTrue();
      expect(component.formulario.disabled).toBeTrue();

      component.onSubmit();
      expect(plantillaServiceSpy.create).not.toHaveBeenCalled();
    });

    it('no llama al servicio y marca los campos obligatorios que faltan', async () => {
      const fixture = await crearComponente(null);
      const component = fixture.componentInstance;

      component.onSubmit();

      expect(plantillaServiceSpy.create).not.toHaveBeenCalled();
      expect(plantillaServiceSpy.update).not.toHaveBeenCalled();
      expect(component.intentoGuardar).toBeTrue();
      expect(component.campoInvalido('tipo')).toBeTrue();
      expect(component.campoInvalido('asunto')).toBeTrue();
      expect(component.campoInvalido('cuerpo')).toBeTrue();
    });
  });

  describe('modo edición', () => {
    it('llama a update y no a create al guardar', async () => {
      const fixture = await crearComponente('1');
      const component = fixture.componentInstance;

      expect(component.modoCreacion).toBeFalse();
      expect(plantillaServiceSpy.getById).toHaveBeenCalledWith('1');
      expect(component.formulario.value.asunto).toBe('Asunto existente');

      component.onSubmit();

      expect(plantillaServiceSpy.update).toHaveBeenCalled();
      expect(plantillaServiceSpy.create).not.toHaveBeenCalled();
      expect(plantillaServiceSpy.update.calls.mostRecent().args[0]).toBe('1');
    });

    it('no incluye el control de tipo en el formulario', async () => {
      const fixture = await crearComponente('1');
      const component = fixture.componentInstance;

      expect(component.formulario.get('tipo')).toBeNull();
    });
  });
});
