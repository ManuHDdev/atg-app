import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

import { PlantillaDocumentoForm } from './plantilla-documento-form';
import { PlantillaDocumentoService } from '../../../services/plantilla-documento.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { ModuloDocumento } from '../../../models/plantilla-documento.model';

describe('PlantillaDocumentoForm', () => {
  let component: PlantillaDocumentoForm;
  let fixture: ComponentFixture<PlantillaDocumentoForm>;
  let plantillaServiceSpy: jasmine.SpyObj<PlantillaDocumentoService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  const petroleras = [{ id: 1, nombre: 'Repsol', activa: true }];

  function pdf(nombre = 'plantilla.pdf'): File {
    return new File(['%PDF-1.4'], nombre, { type: 'application/pdf' });
  }

  async function crearComponente(idRuta: string | null): Promise<void> {
    plantillaServiceSpy = jasmine.createSpyObj('PlantillaDocumentoService', [
      'obtenerPorId', 'crear', 'reemplazarArchivo'
    ]);
    plantillaServiceSpy.obtenerPorId.and.returnValue(of({
      id: 5,
      petroleraId: 1,
      petroleraNombre: 'Repsol',
      modulo: ModuloDocumento.DISPOSITIVOS,
      tipoSolicitud: 'BAJA_DISPOSITIVO',
      nombreArchivo: 'baja.pdf',
      activa: true
    }));

    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    const petroleraServiceSpy = jasmine.createSpyObj('PetroleraService', ['listar']);
    petroleraServiceSpy.listar.and.returnValue(of(petroleras as any));

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [PlantillaDocumentoForm],
      providers: [
        { provide: PlantillaDocumentoService, useValue: plantillaServiceSpy },
        { provide: PetroleraService, useValue: petroleraServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ErrorHandlerService, useValue: jasmine.createSpyObj('ErrorHandlerService', { getMensaje: 'error de prueba' }) },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => idRuta } } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PlantillaDocumentoForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  describe('alta', () => {
    beforeEach(async () => { await crearComponente(null); });

    it('should create', () => {
      expect(component).toBeTruthy();
      expect(component.isEditMode).toBeFalse();
    });

    it('ofrece los tipos de solicitud del módulo seleccionado', () => {
      expect(component.tiposSolicitud.map(t => t.value)).toEqual(['ALTA', 'BAJA', 'DUPLICADO']);

      component.modulo = ModuloDocumento.DISPOSITIVOS;
      expect(component.tiposSolicitud.map(t => t.value))
        .toEqual(['ALTA_DISPOSITIVO', 'SOLICITUD_CREDITO', 'BAJA_DISPOSITIVO', 'CAMBIO_MATRICULA']);
    });

    it('al cambiar de módulo limpia el tipo de solicitud elegido antes', () => {
      component.tipoSolicitud = 'ALTA';
      component.modulo = ModuloDocumento.DISPOSITIVOS;
      component.onModuloChange();

      expect(component.tipoSolicitud).toBe('');
    });

    it('no permite guardar sin petrolera, tipo de solicitud o archivo', () => {
      expect(component.formularioValido).toBeFalse();

      component.petroleraId = 1;
      component.tipoSolicitud = 'ALTA';
      expect(component.formularioValido).toBeFalse();

      component.archivo = pdf();
      expect(component.formularioValido).toBeTrue();
    });

    it('rechaza un archivo que no sea PDF', () => {
      const input = document.createElement('input');
      const noPdf = new File(['x'], 'foto.png', { type: 'image/png' });
      Object.defineProperty(input, 'files', { value: [noPdf] });

      component.onArchivoSeleccionado({ target: input } as unknown as Event);

      expect(component.archivo).toBeNull();
      expect(component.error).toBe('El archivo debe ser un PDF');
    });

    it('crea la plantilla y vuelve al listado', () => {
      plantillaServiceSpy.crear.and.returnValue(of({
        id: 9, petroleraId: 1, modulo: ModuloDocumento.TARJETAS, tipoSolicitud: 'ALTA', activa: true
      }));

      component.petroleraId = 1;
      component.tipoSolicitud = 'ALTA';
      component.archivo = pdf();
      component.guardar();

      expect(plantillaServiceSpy.crear)
        .toHaveBeenCalledWith(1, ModuloDocumento.TARJETAS, 'ALTA', component.archivo!);
      expect(notificationServiceSpy.success).toHaveBeenCalled();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/plantillas-documento']);
    });

    it('muestra el error del backend y no navega si la creación falla', () => {
      plantillaServiceSpy.crear.and.returnValue(throwError(() => new Error('duplicada')));

      component.petroleraId = 1;
      component.tipoSolicitud = 'ALTA';
      component.archivo = pdf();
      component.guardar();

      expect(component.error).toBe('error de prueba');
      expect(component.guardando).toBeFalse();
      expect(routerSpy.navigate).not.toHaveBeenCalled();
    });
  });

  describe('edición', () => {
    beforeEach(async () => { await crearComponente('5'); });

    it('carga la plantilla existente en modo edición', () => {
      expect(component.isEditMode).toBeTrue();
      expect(component.plantillaId).toBe(5);
      expect(component.modulo).toBe(ModuloDocumento.DISPOSITIVOS);
      expect(component.tipoSolicitud).toBe('BAJA_DISPOSITIVO');
      expect(component.nombreArchivoActual).toBe('baja.pdf');
    });

    it('solo permite guardar cuando se elige un archivo nuevo', () => {
      expect(component.formularioValido).toBeFalse();

      component.archivo = pdf('nueva.pdf');
      expect(component.formularioValido).toBeTrue();
    });

    it('reemplaza el archivo en lugar de crear una plantilla nueva', () => {
      plantillaServiceSpy.reemplazarArchivo.and.returnValue(of({
        id: 5, petroleraId: 1, modulo: ModuloDocumento.DISPOSITIVOS, tipoSolicitud: 'BAJA_DISPOSITIVO', activa: true
      }));

      component.archivo = pdf('nueva.pdf');
      component.guardar();

      expect(plantillaServiceSpy.reemplazarArchivo).toHaveBeenCalledWith(5, component.archivo!);
      expect(plantillaServiceSpy.crear).not.toHaveBeenCalled();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/plantillas-documento']);
    });
  });
});
