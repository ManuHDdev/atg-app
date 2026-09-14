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
    estado: EstadoCredito.ENVIADO_PETROLERA
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
      creditoServiceSpy.responderPetrolera.and.returnValue(of(credito as any));

      component.confirmarRespuesta();

      expect(creditoServiceSpy.responderPetrolera).toHaveBeenCalledWith(credito.id, true, component.comentarioRespuesta);
      expect(notificationServiceSpy.success).toHaveBeenCalled();
    });
  });
});
