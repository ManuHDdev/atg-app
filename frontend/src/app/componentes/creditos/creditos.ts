import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CreditoService } from '../../services/credito.service';
import { SocioService } from '../../services/socio.service';
import { EmpresaService } from '../../services/empresa.service';
import { PetroleraService } from '../../services/petrolera.service';
import { NotificationService } from '../../services/notification.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { Credito, CrearCreditoDTO, TipoCredito, EstadoCredito } from '../../models/credito.model';
import { Socio } from '../../models/socio.model';
import { Empresa } from '../../models/empresa.model';
import { Petrolera } from '../../models/petrolera.model';
import { EmailLogs } from '../solicitudes-tarjetas/email-logs/email-logs';
import { SocioAutocomplete } from '../shared/socio-autocomplete/socio-autocomplete';

@Component({
  selector: 'app-creditos',
  standalone: true,
  imports: [CommonModule, FormsModule, EmailLogs, SocioAutocomplete],
  templateUrl: './creditos.html',
  styleUrl: './creditos.css',
})
export class Creditos implements OnInit {
  creditos: Credito[] = [];
  socios: Socio[] = [];
  empresas: Empresa[] = [];
  petroleras: Petrolera[] = [];

  mostrarFormulario = false;
  modoEdicion = false;
  creditoSeleccionado?: Credito;

  nuevoCredito: CrearCreditoDTO = {
    socioId: 0,
    petroleraId: 0,
    tipoCredito: TipoCredito.SOLICITUD_CREDITO
  };

  filtroEstado: EstadoCredito | 'TODOS' = 'TODOS';

  TipoCredito = TipoCredito;
  EstadoCredito = EstadoCredito;

  tiposCredito = [
    { value: TipoCredito.SOLICITUD_CREDITO, label: 'Solicitud de Crédito' },
    { value: TipoCredito.AMPLIACION_CREDITO, label: 'Ampliación de Crédito' },
    { value: TipoCredito.DEVOLUCION_AVAL, label: 'Devolución de Aval' }
  ];

  // Autocomplete Socio (app-socio-autocomplete)
  socioSeleccionado: Socio | null = null;

  // Estado de carga del listado
  loading = false;
  error: string | null = null;

  // Validación del formulario
  intentoGuardar = false;

  // Modal de respuesta petrolera
  mostrarModalRespuesta = false;
  creditoRespondiendo?: Credito;
  aprobandoRespuesta: boolean = false;
  comentarioRespuesta: string = '';

  constructor(
    private creditoService: CreditoService,
    private socioService: SocioService,
    private empresaService: EmpresaService,
    private petroleraService: PetroleraService,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargarCreditos();
    this.cargarSocios();
    this.cargarPetroleras();
  }

  cargarCreditos(): void {
    this.loading = true;
    this.error = null;
    this.creditoService.listarTodos().subscribe({
      next: (data) => {
        this.creditos = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar créditos:', error);
        this.error = this.errorHandler.getMensaje(error, 'créditos');
        this.loading = false;
      }
    });
  }

  cargarSocios(): void {
    this.socioService.getAll().subscribe({
      next: (data) => {
        this.socios = data;
      },
      error: (error) => {
        console.error('Error al cargar socios:', error);
      }
    });
  }

  cargarPetroleras(): void {
    this.petroleraService.listar().subscribe({
      next: (data) => {
        this.petroleras = data.filter(p => p.activa);
      },
      error: (error) => {
        console.error('Error al cargar petroleras:', error);
      }
    });
  }

  onSocioChange(): void {
    if (this.nuevoCredito.socioId) {
      this.empresaService.getBySocioId(this.nuevoCredito.socioId.toString()).subscribe({
        next: (data) => {
          this.empresas = data;
        },
        error: (error) => {
          console.error('Error al cargar empresas:', error);
        }
      });
    }
  }

  abrirFormulario(): void {
    this.mostrarFormulario = true;
    this.modoEdicion = false;
    this.intentoGuardar = false;
    this.nuevoCredito = {
      socioId: 0,
      petroleraId: 0,
      tipoCredito: TipoCredito.SOLICITUD_CREDITO
    };
    this.socioSeleccionado = null;
    this.empresas = [];
  }

  cerrarFormulario(): void {
    this.mostrarFormulario = false;
    this.modoEdicion = false;
    this.intentoGuardar = false;
    this.creditoSeleccionado = undefined;
    this.socioSeleccionado = null;
    this.nuevoCredito.socioId = 0;
    this.empresas = [];
  }

  seleccionarSocioDesdeAutocomplete(socio: Socio): void {
    this.socioSeleccionado = socio;
    this.nuevoCredito.socioId = Number(socio.id);
    this.onSocioChange();
  }

  guardarCredito(): void {
    this.intentoGuardar = true;

    if (!this.nuevoCredito.socioId || !this.nuevoCredito.petroleraId) {
      this.notificationService.error('Por favor complete todos los campos obligatorios');
      return;
    }

    this.creditoService.crear(this.nuevoCredito).subscribe({
      next: (credito) => {
        this.notificationService.success('Crédito creado exitosamente');
        this.cerrarFormulario();
        this.cargarCreditos();
      },
      error: (error) => {
        console.error('Error al crear crédito:', error);
        this.notificationService.error(this.errorHandler.getMensaje(error, 'crédito'));
      }
    });
  }

  verDetalle(credito: Credito): void {
    this.creditoSeleccionado = credito;
  }

  enviarAPetrolera(credito: Credito): void {
    if (confirm('¿Está seguro de enviar este crédito a la petrolera?')) {
      this.creditoService.enviarAPetrolera(credito.id!).subscribe({
        next: () => {
          this.notificationService.success('Crédito enviado a la petrolera exitosamente');
          this.cargarCreditos();
        },
        error: (error) => {
          console.error('Error al enviar crédito:', error);
          this.notificationService.error(this.errorHandler.getMensaje(error, 'crédito'));
        }
      });
    }
  }

  abrirModalRespuesta(credito: Credito, aprobado: boolean): void {
    this.creditoRespondiendo = credito;
    this.aprobandoRespuesta = aprobado;
    this.comentarioRespuesta = '';
    this.mostrarModalRespuesta = true;
  }

  cerrarModalRespuesta(): void {
    this.mostrarModalRespuesta = false;
    this.creditoRespondiendo = undefined;
    this.comentarioRespuesta = '';
  }

  confirmarRespuesta(): void {
    if (!this.creditoRespondiendo) return;
    if (!confirm('¿Está seguro de ' + (this.aprobandoRespuesta ? 'aprobar' : 'denegar') + ' este crédito?')) return;
    this.creditoService.responderPetrolera(
      this.creditoRespondiendo.id!,
      this.aprobandoRespuesta,
      this.comentarioRespuesta
    ).subscribe({
      next: () => {
        this.notificationService.success(`Crédito ${this.aprobandoRespuesta ? 'aprobado' : 'denegado'} exitosamente`);
        this.cerrarModalRespuesta();
        this.creditoSeleccionado = undefined;
        this.cargarCreditos();
      },
      error: (error) => {
        console.error('Error al responder crédito:', error);
        this.notificationService.error(this.errorHandler.getMensaje(error, 'crédito'));
      }
    });
  }

  get creditosFiltrados(): Credito[] {
    if (this.filtroEstado === 'TODOS') {
      return this.creditos;
    }
    return this.creditos.filter(c => c.estado === this.filtroEstado);
  }

  getEstadoClass(estado: EstadoCredito): string {
    switch (estado) {
      case EstadoCredito.PENDIENTE:
        return 'badge-warning';
      case EstadoCredito.ENVIADO_PETROLERA:
        return 'badge-info';
      case EstadoCredito.APROBADO:
        return 'badge-success';
      case EstadoCredito.DENEGADO:
        return 'badge-danger';
      case EstadoCredito.COMPLETADO_APROBADO:
        return 'badge-success';
      case EstadoCredito.COMPLETADO_DENEGADO:
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }

  getEstadoTexto(estado: EstadoCredito): string {
    switch (estado) {
      case EstadoCredito.PENDIENTE: return 'Pendiente';
      case EstadoCredito.ENVIADO_PETROLERA: return 'Enviado a Petrolera';
      case EstadoCredito.APROBADO: return 'Aprobado';
      case EstadoCredito.DENEGADO: return 'Denegado';
      case EstadoCredito.COMPLETADO_APROBADO: return 'Completado (Aprobado)';
      case EstadoCredito.COMPLETADO_DENEGADO: return 'Completado (Denegado)';
      default: return estado;
    }
  }

  getTipoCreditoLabel(tipo: TipoCredito): string {
    const found = this.tiposCredito.find(t => t.value === tipo);
    return found ? found.label : tipo;
  }

  onTipoCreditoChange(): void {
    if (this.nuevoCredito.tipoCredito === TipoCredito.DEVOLUCION_AVAL) {
      this.nuevoCredito.monto = undefined;
    }
  }
}
