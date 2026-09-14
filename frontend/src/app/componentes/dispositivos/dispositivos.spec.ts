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
  let petroleraServiceSpy: jasmine.SpyObj<PetroleraService>;

  // Cepsa/Moeve: opera y admite crédito. Repsol: opera pero sin crédito.
  // Galp: no opera con dispositivos. Sin configurar: flags a null => sin restricción.
  const petroleras = [
    { id: 1, nombre: 'Cepsa (Moeve)', activa: true, operaDispositivos: true, permiteCreditoDispositivo: true },
    { id: 2, nombre: 'Repsol', activa: true, operaDispositivos: true, permiteCreditoDispositivo: false },
    { id: 3, nombre: 'Galp', activa: true, operaDispositivos: false, permiteCreditoDispositivo: false },
    { id: 4, nombre: 'Sin configurar', activa: true, operaDispositivos: null, permiteCreditoDispositivo: null },
    { id: 5, nombre: 'Inactiva', activa: false, operaDispositivos: true, permiteCreditoDispositivo: true }
  ];

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

    petroleraServiceSpy = jasmine.createSpyObj('PetroleraService', ['listar']);
    petroleraServiceSpy.listar.and.returnValue(of(petroleras as any));

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

  describe('petroleras disponibles', () => {
    it('solo ofrece petroleras activas que operan con dispositivos', () => {
      expect(component.petroleras.map(p => p.id)).toEqual([1, 2, 4]);
    });

    it('restringe la lista a las que admiten crédito cuando el tipo es Solicitud de Crédito', () => {
      component.nuevaSolicitud.tipoSolicitud = TipoSolicitudDispositivo.SOLICITUD_CREDITO;

      expect(component.petroleras.map(p => p.id)).toEqual([1, 4]);
    });

    it('deselecciona la petrolera si deja de ser válida al cambiar de tipo', () => {
      component.nuevaSolicitud.petroleraId = 2; // Repsol: no admite crédito
      component.nuevaSolicitud.tipoSolicitud = TipoSolicitudDispositivo.SOLICITUD_CREDITO;

      component.onTipoSolicitudChange();

      expect(component.nuevaSolicitud.petroleraId).toBe(0);
      expect(notificationServiceSpy.warning).toHaveBeenCalled();
    });

    it('mantiene la petrolera si sigue siendo válida al cambiar de tipo', () => {
      component.nuevaSolicitud.petroleraId = 1; // Cepsa/Moeve: admite crédito
      component.nuevaSolicitud.tipoSolicitud = TipoSolicitudDispositivo.SOLICITUD_CREDITO;

      component.onTipoSolicitudChange();

      expect(component.nuevaSolicitud.petroleraId).toBe(1);
      expect(notificationServiceSpy.warning).not.toHaveBeenCalled();
    });
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

      expect(dispositivoServiceSpy.responderPetrolera).toHaveBeenCalledWith(
        solicitud.id, true, component.comentarioRespuesta, null);
      expect(notificationServiceSpy.success).toHaveBeenCalled();
    });
  });

  describe('importe concedido en el modal de respuesta', () => {
    const solicitudCredito = {
      id: 2,
      socioId: 10,
      petroleraId: 20,
      tipoSolicitud: TipoSolicitudDispositivo.SOLICITUD_CREDITO,
      estado: EstadoSolicitudDispositivo.ENVIADO_PETROLERA,
      monto: 2000
    };

    it('precarga el importe concedido con el solicitado al aprobar un credito', () => {
      component.abrirModalRespuesta(solicitudCredito as any, true);

      expect(component.montoConcedidoRespuesta).toBe(2000);
    });

    it('no precarga importe al denegar', () => {
      component.abrirModalRespuesta(solicitudCredito as any, false);

      expect(component.montoConcedidoRespuesta).toBeNull();
    });

    it('solo exige importe concedido para el tipo Solicitud de Credito', () => {
      expect(component.requiereImporteConcedido(solicitudCredito as any)).toBeTrue();
      expect(component.requiereImporteConcedido(solicitud as any)).toBeFalse();
    });

    it('no llama al backend si se aprueba un credito sin importe concedido', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      component.abrirModalRespuesta(solicitudCredito as any, true);
      component.montoConcedidoRespuesta = null;

      component.confirmarRespuesta();

      expect(component.intentoGuardar).toBeTrue();
      expect(dispositivoServiceSpy.responderPetrolera).not.toHaveBeenCalled();
      expect(notificationServiceSpy.error).toHaveBeenCalled();
    });

    it('envia el importe concedido cuando difiere del solicitado', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      dispositivoServiceSpy.responderPetrolera.and.returnValue(of(solicitudCredito as any));
      component.abrirModalRespuesta(solicitudCredito as any, true);
      component.montoConcedidoRespuesta = 4000;

      component.confirmarRespuesta();

      expect(dispositivoServiceSpy.responderPetrolera).toHaveBeenCalledWith(solicitudCredito.id, true, '', 4000);
    });

    it('un alta de dispositivo se aprueba sin importe concedido', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      dispositivoServiceSpy.responderPetrolera.and.returnValue(of(solicitud as any));
      component.abrirModalRespuesta(solicitud as any, true);

      component.confirmarRespuesta();

      expect(dispositivoServiceSpy.responderPetrolera).toHaveBeenCalledWith(solicitud.id, true, '', null);
    });

    it('detecta cuando el importe concedido difiere del solicitado', () => {
      expect(component.importeDifiere({ ...solicitudCredito, montoConcedido: 4000 } as any)).toBeTrue();
      expect(component.importeDifiere({ ...solicitudCredito, montoConcedido: 2000 } as any)).toBeFalse();
      expect(component.importeDifiere(solicitudCredito as any)).toBeFalse();
    });
  });
});
