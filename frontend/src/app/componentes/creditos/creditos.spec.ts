import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Creditos } from './creditos';
import { CreditoService } from '../../services/credito.service';
import { SocioService } from '../../services/socio.service';
import { EmpresaService } from '../../services/empresa.service';
import { PetroleraService } from '../../services/petrolera.service';
import { NotificationService } from '../../services/notification.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { EstadoCredito, TipoCredito } from '../../models/credito.model';

describe('Creditos', () => {
  let component: Creditos;
  let fixture: ComponentFixture<Creditos>;
  let creditoServiceSpy: jasmine.SpyObj<CreditoService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  const credito = {
    id: 1,
    socioId: 10,
    petroleraId: 20,
    tipoCredito: TipoCredito.SOLICITUD_CREDITO,
    estado: EstadoCredito.ENVIADO_PETROLERA,
    monto: 12000
  };

  beforeEach(async () => {
    creditoServiceSpy = jasmine.createSpyObj('CreditoService', [
      'listarTodos', 'crear', 'enviarAPetrolera', 'responderPetrolera'
    ]);
    creditoServiceSpy.listarTodos.and.returnValue(of([]));

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    const socioServiceSpy = jasmine.createSpyObj('SocioService', ['getAll']);
    socioServiceSpy.getAll.and.returnValue(of([]));

    const empresaServiceSpy = jasmine.createSpyObj('EmpresaService', ['getBySocioId']);
    empresaServiceSpy.getBySocioId.and.returnValue(of([]));

    const petroleraServiceSpy = jasmine.createSpyObj('PetroleraService', ['listar']);
    petroleraServiceSpy.listar.and.returnValue(of([
      { id: 1, nombre: 'Cepsa (Moeve)', activa: true, operaCreditos: true },
      { id: 2, nombre: 'Solred', activa: true, operaCreditos: false },
      { id: 3, nombre: 'Sin configurar', activa: true, operaCreditos: null },
      { id: 4, nombre: 'Inactiva', activa: false, operaCreditos: true }
    ] as any));

    await TestBed.configureTestingModule({
      imports: [Creditos],
      providers: [
        { provide: CreditoService, useValue: creditoServiceSpy },
        { provide: SocioService, useValue: socioServiceSpy },
        { provide: EmpresaService, useValue: empresaServiceSpy },
        { provide: PetroleraService, useValue: petroleraServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: ErrorHandlerService, useValue: jasmine.createSpyObj('ErrorHandlerService', { getMensaje: 'error de prueba' }) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Creditos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('solo ofrece petroleras activas que operan con créditos (null = sin restricción)', () => {
    expect(component.petroleras.map(p => p.id)).toEqual([1, 3]);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('carga la lista de créditos al iniciar', () => {
    expect(creditoServiceSpy.listarTodos).toHaveBeenCalled();
  });

  describe('guardarCredito', () => {
    it('no llama al servicio ni cierra el formulario si faltan campos obligatorios', () => {
      component.nuevoCredito.socioId = 0;
      component.nuevoCredito.petroleraId = 0;

      component.guardarCredito();

      expect(component.intentoGuardar).toBeTrue();
      expect(creditoServiceSpy.crear).not.toHaveBeenCalled();
      expect(notificationServiceSpy.error).toHaveBeenCalled();
    });

    it('crea el crédito y notifica éxito cuando los campos obligatorios están completos', () => {
      component.nuevoCredito.socioId = 10;
      component.nuevoCredito.petroleraId = 20;
      creditoServiceSpy.crear.and.returnValue(of(credito as any));

      component.guardarCredito();

      expect(creditoServiceSpy.crear).toHaveBeenCalledWith(component.nuevoCredito);
      expect(notificationServiceSpy.success).toHaveBeenCalled();
      expect(component.mostrarFormulario).toBeFalse();
    });

    it('notifica el error del backend si la creación falla', () => {
      component.nuevoCredito.socioId = 10;
      component.nuevoCredito.petroleraId = 20;
      creditoServiceSpy.crear.and.returnValue(throwError(() => new Error('fallo')));

      component.guardarCredito();

      expect(notificationServiceSpy.error).toHaveBeenCalledWith('error de prueba');
    });
  });

  describe('confirmarRespuesta', () => {
    beforeEach(() => {
      component.creditoRespondiendo = credito as any;
    });

    it('no llama al backend si el usuario cancela la confirmación', () => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.confirmarRespuesta();

      expect(creditoServiceSpy.responderPetrolera).not.toHaveBeenCalled();
    });

    it('aprueba el crédito y notifica éxito si el usuario confirma', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      component.aprobandoRespuesta = true;
      // Aprobar exige importe concedido: se precarga con el solicitado
      component.montoConcedidoRespuesta = credito.monto;
      creditoServiceSpy.responderPetrolera.and.returnValue(of(credito as any));

      component.confirmarRespuesta();

      expect(creditoServiceSpy.responderPetrolera).toHaveBeenCalledWith(credito.id, true, '', credito.monto);
      expect(notificationServiceSpy.success).toHaveBeenCalled();
    });
  });

  describe('importe concedido en el modal de respuesta', () => {
    it('precarga el importe concedido con el importe solicitado al aprobar', () => {
      component.abrirModalRespuesta(credito as any, true);

      expect(component.montoConcedidoRespuesta).toBe(12000);
      expect(component.intentoGuardar).toBeFalse();
    });

    it('no precarga importe al denegar', () => {
      component.abrirModalRespuesta(credito as any, false);

      expect(component.montoConcedidoRespuesta).toBeNull();
    });

    it('oculta el campo al denegar y lo muestra al aprobar (salvo devolucion de aval)', () => {
      expect(component.requiereImporteConcedido(credito as any)).toBeTrue();
      expect(component.requiereImporteConcedido(
        { ...credito, tipoCredito: TipoCredito.DEVOLUCION_AVAL } as any)).toBeFalse();
    });

    it('exige el importe concedido al aprobar y no llama al backend si falta', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      component.abrirModalRespuesta(credito as any, true);
      component.montoConcedidoRespuesta = null;

      component.confirmarRespuesta();

      expect(component.intentoGuardar).toBeTrue();
      expect(creditoServiceSpy.responderPetrolera).not.toHaveBeenCalled();
      expect(notificationServiceSpy.error).toHaveBeenCalled();
    });

    it('rechaza un importe concedido de 0 o negativo', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      component.abrirModalRespuesta(credito as any, true);
      component.montoConcedidoRespuesta = 0;

      component.confirmarRespuesta();

      expect(creditoServiceSpy.responderPetrolera).not.toHaveBeenCalled();
    });

    it('envia null como importe concedido al denegar', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      creditoServiceSpy.responderPetrolera.and.returnValue(of(credito as any));
      component.abrirModalRespuesta(credito as any, false);
      component.comentarioRespuesta = 'Denegado';

      component.confirmarRespuesta();

      expect(creditoServiceSpy.responderPetrolera).toHaveBeenCalledWith(credito.id, false, 'Denegado', null);
    });

    it('envia el importe concedido cuando difiere del solicitado', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      creditoServiceSpy.responderPetrolera.and.returnValue(of(credito as any));
      component.abrirModalRespuesta(credito as any, true);
      component.montoConcedidoRespuesta = 8000;

      component.confirmarRespuesta();

      expect(creditoServiceSpy.responderPetrolera).toHaveBeenCalledWith(credito.id, true, '', 8000);
    });

    it('detecta cuando el importe concedido difiere del solicitado', () => {
      expect(component.importeDifiere({ ...credito, montoConcedido: 8000 } as any)).toBeTrue();
      expect(component.importeDifiere({ ...credito, montoConcedido: 12000 } as any)).toBeFalse();
      expect(component.importeDifiere(credito as any)).toBeFalse();
    });
  });
});
