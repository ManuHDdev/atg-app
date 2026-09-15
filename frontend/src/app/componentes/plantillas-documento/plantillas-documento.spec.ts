import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';

import { PlantillasDocumento } from './plantillas-documento';
import { PlantillaDocumentoService } from '../../services/plantilla-documento.service';
import { PetroleraService } from '../../services/petrolera.service';
import { NotificationService } from '../../services/notification.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { ModuloDocumento, PlantillaDocumento } from '../../models/plantilla-documento.model';

describe('PlantillasDocumento', () => {
  let component: PlantillasDocumento;
  let fixture: ComponentFixture<PlantillasDocumento>;
  let plantillaServiceSpy: jasmine.SpyObj<PlantillaDocumentoService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let routerSpy: jasmine.SpyObj<Router>;

  const petroleras = [
    { id: 1, nombre: 'Repsol', activa: true },
    { id: 2, nombre: 'Cepsa', activa: true },
    { id: 3, nombre: 'Petrolera de baja', activa: false }
  ];

  const plantillas: PlantillaDocumento[] = [
    {
      id: 1,
      petroleraId: 1,
      petroleraNombre: 'Repsol',
      modulo: ModuloDocumento.TARJETAS,
      tipoSolicitud: 'ALTA',
      nombreArchivo: 'alta-tarjeta.pdf',
      activa: true
    },
    {
      id: 2,
      petroleraId: 2,
      petroleraNombre: 'Cepsa',
      modulo: ModuloDocumento.DISPOSITIVOS,
      tipoSolicitud: 'CAMBIO_MATRICULA',
      nombreArchivo: 'cambio-matricula.pdf',
      activa: false
    }
  ];

  beforeEach(async () => {
    plantillaServiceSpy = jasmine.createSpyObj('PlantillaDocumentoService', [
      'listarTodas', 'cambiarEstado', 'eliminar', 'descargarArchivo'
    ]);
    plantillaServiceSpy.listarTodas.and.returnValue(of(plantillas));

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    const petroleraServiceSpy = jasmine.createSpyObj('PetroleraService', ['listar']);
    petroleraServiceSpy.listar.and.returnValue(of(petroleras as any));

    await TestBed.configureTestingModule({
      imports: [PlantillasDocumento],
      providers: [
        { provide: PlantillaDocumentoService, useValue: plantillaServiceSpy },
        { provide: PetroleraService, useValue: petroleraServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ErrorHandlerService, useValue: jasmine.createSpyObj('ErrorHandlerService', { getMensaje: 'error de prueba' }) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PlantillasDocumento);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('carga las plantillas y solo las petroleras activas al iniciar', () => {
    expect(plantillaServiceSpy.listarTodas).toHaveBeenCalled();
    expect(component.plantillasFiltradas.length).toBe(2);
    expect(component.petroleras.map(p => p.nombre)).toEqual(['Repsol', 'Cepsa']);
  });

  it('filtra por módulo', () => {
    component.filtroModulo = ModuloDocumento.DISPOSITIVOS;
    component.aplicarFiltros();

    expect(component.plantillasFiltradas.length).toBe(1);
    expect(component.plantillasFiltradas[0].tipoSolicitud).toBe('CAMBIO_MATRICULA');
  });

  it('filtra por petrolera y por estado', () => {
    component.filtroPetroleraId = '1';
    component.aplicarFiltros();
    expect(component.plantillasFiltradas.length).toBe(1);

    component.limpiarFiltros();
    component.filtroActiva = 'false';
    component.aplicarFiltros();
    expect(component.plantillasFiltradas.length).toBe(1);
    expect(component.plantillasFiltradas[0].activa).toBeFalse();
  });

  it('traduce el tipo de solicitud al idioma del módulo correspondiente', () => {
    expect(component.getTipoSolicitudLabel(plantillas[0])).toBe('Alta de tarjeta');
    expect(component.getTipoSolicitudLabel(plantillas[1])).toBe('Cambio de matrícula');
  });

  it('cambiarEstado invierte el estado de la plantilla', () => {
    const plantilla = { ...plantillas[0] };
    plantillaServiceSpy.cambiarEstado.and.returnValue(of({ ...plantilla, activa: false }));

    component.cambiarEstado(plantilla);

    expect(plantillaServiceSpy.cambiarEstado).toHaveBeenCalledWith(1, false);
    expect(plantilla.activa).toBeFalse();
  });

  it('cambiarEstado avisa por notificación si el backend falla y no toca el estado local', () => {
    const plantilla = { ...plantillas[0] };
    plantillaServiceSpy.cambiarEstado.and.returnValue(throwError(() => new Error('fallo')));

    component.cambiarEstado(plantilla);

    expect(notificationServiceSpy.error).toHaveBeenCalled();
    expect(plantilla.activa).toBeTrue();
  });

  it('eliminarPlantilla recarga el listado tras confirmar', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    plantillaServiceSpy.eliminar.and.returnValue(of(void 0));
    plantillaServiceSpy.listarTodas.calls.reset();

    component.eliminarPlantilla(1);

    expect(plantillaServiceSpy.eliminar).toHaveBeenCalledWith(1);
    expect(plantillaServiceSpy.listarTodas).toHaveBeenCalled();
  });

  it('eliminarPlantilla no llama al backend si se cancela la confirmación', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.eliminarPlantilla(1);

    expect(plantillaServiceSpy.eliminar).not.toHaveBeenCalled();
  });

  it('muestra el mensaje de error cuando falla la carga', () => {
    plantillaServiceSpy.listarTodas.and.returnValue(throwError(() => new Error('fallo')));

    component.cargarPlantillas();

    expect(component.error).toBe('error de prueba');
    expect(component.loading).toBeFalse();
  });
});
