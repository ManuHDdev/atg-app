import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Dispositivos } from './dispositivos';
import { DispositivoService } from '../../services/dispositivo.service';
import { SocioService } from '../../services/socio.service';
import { EmpresaService } from '../../services/empresa.service';
import { PetroleraService } from '../../services/petrolera.service';
import { NotificationService } from '../../services/notification.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { EstadoSolicitudDispositivo, TipoSolicitudDispositivo } from '../../models/dispositivo.model';

describe('Dispositivos', () => {
  let component: Dispositivos;
  let fixture: ComponentFixture<Dispositivos>;
  let dispositivoServiceSpy: jasmine.SpyObj<DispositivoService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  const solicitud = {
    id: 1,
    socioId: 10,
    petroleraId: 20,
    tipoSolicitud: TipoSolicitudDispositivo.ALTA_DISPOSITIVO,
    estado: EstadoSolicitudDispositivo.ENVIADO_PETROLERA,
    matricula: '1234ABC'
  };

  beforeEach(async () => {
    dispositivoServiceSpy = jasmine.createSpyObj('DispositivoService', [
      'listarSolicitudes', 'crearSolicitud', 'enviarAPetrolera', 'responderPetrolera', 'listarActivosPorSocio'
    ]);
    dispositivoServiceSpy.listarSolicitudes.and.returnValue(of([]));
    dispositivoServiceSpy.listarActivosPorSocio.and.returnValue(of([]));

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    const socioServiceSpy = jasmine.createSpyObj('SocioService', ['getAll']);
    socioServiceSpy.getAll.and.returnValue(of([]));

    const empresaServiceSpy = jasmine.createSpyObj('EmpresaService', ['getBySocioId']);
    empresaServiceSpy.getBySocioId.and.returnValue(of([]));

    const petroleraServiceSpy = jasmine.createSpyObj('PetroleraService', ['listar']);
    petroleraServiceSpy.listar.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Dispositivos],
      providers: [
        { provide: DispositivoService, useValue: dispositivoServiceSpy },
        { provide: SocioService, useValue: socioServiceSpy },
        { provide: EmpresaService, useValue: empresaServiceSpy },
        { provide: PetroleraService, useValue: petroleraServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: ErrorHandlerService, useValue: jasmine.createSpyObj('ErrorHandlerService', { getMensaje: 'error de prueba' }) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Dispositivos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('carga la lista de solicitudes al iniciar', () => {
    expect(dispositivoServiceSpy.listarSolicitudes).toHaveBeenCalled();
  });

  describe('guardarSolicitud', () => {
    it('no llama al servicio si faltan socio o petrolera', () => {
      component.nuevaSolicitud.socioId = 0;
      component.nuevaSolicitud.petroleraId = 0;

      component.guardarSolicitud();

      expect(dispositivoServiceSpy.crearSolicitud).not.toHaveBeenCalled();
      expect(notificationServiceSpy.error).toHaveBeenCalled();
    });

    it('exige matrícula para una alta de dispositivo', () => {
      component.nuevaSolicitud.socioId = 10;
      component.nuevaSolicitud.petroleraId = 20;
      component.nuevaSolicitud.tipoSolicitud = TipoSolicitudDispositivo.ALTA_DISPOSITIVO;
      component.nuevaSolicitud.matricula = '';

      component.guardarSolicitud();

      expect(dispositivoServiceSpy.crearSolicitud).not.toHaveBeenCalled();
      expect(notificationServiceSpy.error).toHaveBeenCalled();
    });

    it('crea la solicitud y notifica éxito cuando los datos son válidos', () => {
      component.nuevaSolicitud.socioId = 10;
      component.nuevaSolicitud.petroleraId = 20;
      component.nuevaSolicitud.tipoSolicitud = TipoSolicitudDispositivo.ALTA_DISPOSITIVO;
      component.nuevaSolicitud.matricula = '1234ABC';
      dispositivoServiceSpy.crearSolicitud.and.returnValue(of(solicitud as any));

      component.guardarSolicitud();

      expect(dispositivoServiceSpy.crearSolicitud).toHaveBeenCalledWith(component.nuevaSolicitud);
      expect(notificationServiceSpy.success).toHaveBeenCalled();
      expect(component.mostrarFormulario).toBeFalse();
    });

    it('notifica el error del backend si la creación falla', () => {
      component.nuevaSolicitud.socioId = 10;
      component.nuevaSolicitud.petroleraId = 20;
      component.nuevaSolicitud.tipoSolicitud = TipoSolicitudDispositivo.ALTA_DISPOSITIVO;
      component.nuevaSolicitud.matricula = '1234ABC';
      dispositivoServiceSpy.crearSolicitud.and.returnValue(throwError(() => new Error('fallo')));

      component.guardarSolicitud();

      expect(notificationServiceSpy.error).toHaveBeenCalledWith('error de prueba');
    });
  });

  describe('confirmarRespuesta', () => {
    beforeEach(() => {
      component.solicitudRespondiendo = solicitud as any;
    });

    it('no llama al backend si el usuario cancela la confirmación', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.confirmarRespuesta();

      expect(dispositivoServiceSpy.responderPetrolera).not.toHaveBeenCalled();
    });

    it('aprueba la solicitud y notifica éxito si el usuario confirma', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      component.aprobandoRespuesta = true;
      dispositivoServiceSpy.responderPetrolera.and.returnValue(of(solicitud as any));

      component.confirmarRespuesta();

      expect(dispositivoServiceSpy.responderPetrolera).toHaveBeenCalledWith(solicitud.id, true, component.comentarioRespuesta);
      expect(notificationServiceSpy.success).toHaveBeenCalled();
    });
  });
});
