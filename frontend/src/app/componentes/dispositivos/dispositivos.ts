import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DispositivoService } from '../../services/dispositivo.service';
import { SocioService } from '../../services/socio.service';
import { EmpresaService } from '../../services/empresa.service';
import { PetroleraService } from '../../services/petrolera.service';
import { NotificationService } from '../../services/notification.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import {
  Dispositivo,
  SolicitudDispositivo,
  CrearSolicitudDispositivoDTO,
  TipoSolicitudDispositivo,
  EstadoSolicitudDispositivo
} from '../../models/dispositivo.model';
import { Socio } from '../../models/socio.model';
import { Empresa } from '../../models/empresa.model';
import { Petrolera } from '../../models/petrolera.model';
import { EmailLogs } from '../solicitudes-tarjetas/email-logs/email-logs';
import { SocioAutocomplete } from '../shared/socio-autocomplete/socio-autocomplete';

@Component({
  selector: 'app-dispositivos',
  standalone: true,
  imports: [CommonModule, FormsModule, EmailLogs, SocioAutocomplete],
  templateUrl: './dispositivos.html',
  styleUrl: './dispositivos.css',
})
export class Dispositivos implements OnInit {
  solicitudes: SolicitudDispositivo[] = [];
  socios: Socio[] = [];
  empresas: Empresa[] = [];
  petroleras: Petrolera[] = [];
  dispositivosSocio: Dispositivo[] = [];

  mostrarFormulario = false;
  solicitudSeleccionada?: SolicitudDispositivo;

  nuevaSolicitud: CrearSolicitudDispositivoDTO = {
    socioId: 0,
    petroleraId: 0,
    tipoSolicitud: TipoSolicitudDispositivo.ALTA_DISPOSITIVO
  };

  filtroEstado: EstadoSolicitudDispositivo | 'TODOS' = 'TODOS';
  filtroTipo: TipoSolicitudDispositivo | 'TODOS' = 'TODOS';

  TipoSolicitud = TipoSolicitudDispositivo;
  EstadoSolicitud = EstadoSolicitudDispositivo;

  tiposSolicitud = [
    { value: TipoSolicitudDispositivo.ALTA_DISPOSITIVO, label: 'Alta de Dispositivo' },
    { value: TipoSolicitudDispositivo.SOLICITUD_CREDITO, label: 'Solicitud de Crédito' },
    { value: TipoSolicitudDispositivo.BAJA_DISPOSITIVO, label: 'Baja de Dispositivo' },
    { value: TipoSolicitudDispositivo.CAMBIO_MATRICULA, label: 'Cambio de Matrícula' }
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
  solicitudRespondiendo?: SolicitudDispositivo;
  aprobandoRespuesta: boolean = false;
  comentarioRespuesta: string = '';

  constructor(
    private dispositivoService: DispositivoService,
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
    this.cargarSolicitudes();
    this.cargarSocios();
    this.cargarPetroleras();
  }

  cargarSolicitudes(): void {
    this.loading = true;
    this.error = null;
    this.dispositivoService.listarSolicitudes().subscribe({
      next: (data) => {
        this.solicitudes = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar solicitudes:', error);
        this.error = this.errorHandler.getMensaje(error, 'solicitudes');
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
    if (this.nuevaSolicitud.socioId) {
      this.empresaService.getBySocioId(this.nuevaSolicitud.socioId.toString()).subscribe({
        next: (data) => {
          this.empresas = data;
        },
        error: (error) => {
          console.error('Error al cargar empresas:', error);
        }
      });
      this.cargarDispositivosSocio();
    }
  }

  cargarDispositivosSocio(): void {
    if (this.nuevaSolicitud.socioId) {
      this.dispositivoService.listarActivosPorSocio(this.nuevaSolicitud.socioId).subscribe({
        next: (data) => {
          this.dispositivosSocio = data;
        },
        error: (error) => {
          console.error('Error al cargar dispositivos del socio:', error);
          this.dispositivosSocio = [];
        }
      });
    }
  }

  abrirFormulario(): void {
    this.mostrarFormulario = true;
    this.intentoGuardar = false;
    this.nuevaSolicitud = {
      socioId: 0,
      petroleraId: 0,
      tipoSolicitud: TipoSolicitudDispositivo.ALTA_DISPOSITIVO
    };
    this.socioSeleccionado = null;
    this.empresas = [];
    this.dispositivosSocio = [];
  }

  cerrarFormulario(): void {
    this.mostrarFormulario = false;
    this.intentoGuardar = false;
    this.solicitudSeleccionada = undefined;
    this.socioSeleccionado = null;
    this.nuevaSolicitud.socioId = 0;
    this.empresas = [];
    this.dispositivosSocio = [];
  }

  seleccionarSocioDesdeAutocomplete(socio: Socio): void {
    this.socioSeleccionado = socio;
    this.nuevaSolicitud.socioId = Number(socio.id);
    this.onSocioChange();
  }

  onTipoSolicitudChange(): void {
    // Limpiar campos condicionales al cambiar tipo
    this.nuevaSolicitud.dispositivoId = undefined;
    this.nuevaSolicitud.matricula = undefined;
    this.nuevaSolicitud.matriculaDestino = undefined;
    this.nuevaSolicitud.monto = undefined;
  }

  guardarSolicitud(): void {
    this.intentoGuardar = true;

    if (!this.nuevaSolicitud.socioId || !this.nuevaSolicitud.petroleraId) {
      this.notificationService.error('Por favor complete todos los campos obligatorios');
      return;
    }

    const tipo = this.nuevaSolicitud.tipoSolicitud;

    // Validaciones por tipo
    if (tipo === TipoSolicitudDispositivo.ALTA_DISPOSITIVO) {
      if (!this.nuevaSolicitud.matricula || !this.nuevaSolicitud.matricula.trim()) {
        this.notificationService.error('La matrícula es obligatoria para Alta de Dispositivo');
        return;
      }
    }

    if (tipo === TipoSolicitudDispositivo.SOLICITUD_CREDITO) {
      if (!this.nuevaSolicitud.dispositivoId) {
        this.notificationService.error('Debe seleccionar un dispositivo activo');
        return;
      }
      if (!this.nuevaSolicitud.monto || this.nuevaSolicitud.monto <= 0) {
        this.notificationService.error('El monto es obligatorio y debe ser mayor a 0');
        return;
      }
    }

    if (tipo === TipoSolicitudDispositivo.BAJA_DISPOSITIVO) {
      if (!this.nuevaSolicitud.dispositivoId) {
        this.notificationService.error('Debe seleccionar un dispositivo activo');
        return;
      }
    }

    if (tipo === TipoSolicitudDispositivo.CAMBIO_MATRICULA) {
      if (!this.nuevaSolicitud.dispositivoId) {
        this.notificationService.error('Debe seleccionar un dispositivo activo');
        return;
      }
      if (!this.nuevaSolicitud.matriculaDestino || !this.nuevaSolicitud.matriculaDestino.trim()) {
        this.notificationService.error('La matrícula destino es obligatoria para Cambio de Matrícula');
        return;
      }
    }

    this.dispositivoService.crearSolicitud(this.nuevaSolicitud).subscribe({
      next: () => {
        this.notificationService.success('Solicitud creada exitosamente');
        this.cerrarFormulario();
        this.cargarSolicitudes();
      },
      error: (error) => {
        console.error('Error al crear solicitud:', error);
        this.notificationService.error(this.errorHandler.getMensaje(error, 'solicitud'));
      }
    });
  }

  verDetalle(solicitud: SolicitudDispositivo): void {
    this.solicitudSeleccionada = solicitud;
  }

  enviarAPetrolera(solicitud: SolicitudDispositivo): void {
    if (confirm('¿Está seguro de enviar esta solicitud a la petrolera?')) {
      this.dispositivoService.enviarAPetrolera(solicitud.id!).subscribe({
        next: () => {
          this.notificationService.success('Solicitud enviada a la petrolera exitosamente');
          this.cargarSolicitudes();
        },
        error: (error) => {
          console.error('Error al enviar solicitud:', error);
          this.notificationService.error(this.errorHandler.getMensaje(error, 'solicitud'));
        }
      });
    }
  }

  abrirModalRespuesta(solicitud: SolicitudDispositivo, aprobado: boolean): void {
    this.solicitudRespondiendo = solicitud;
    this.aprobandoRespuesta = aprobado;
    this.comentarioRespuesta = '';
    this.mostrarModalRespuesta = true;
  }

  cerrarModalRespuesta(): void {
    this.mostrarModalRespuesta = false;
    this.solicitudRespondiendo = undefined;
    this.comentarioRespuesta = '';
  }

  confirmarRespuesta(): void {
    if (!this.solicitudRespondiendo) return;
    if (!confirm('¿Está seguro de ' + (this.aprobandoRespuesta ? 'aprobar' : 'denegar') + ' esta solicitud?')) return;
    this.dispositivoService.responderPetrolera(
      this.solicitudRespondiendo.id!,
      this.aprobandoRespuesta,
      this.comentarioRespuesta
    ).subscribe({
      next: () => {
        this.notificationService.success(`Solicitud ${this.aprobandoRespuesta ? 'aprobada' : 'denegada'} exitosamente`);
        this.cerrarModalRespuesta();
        this.solicitudSeleccionada = undefined;
        this.cargarSolicitudes();
      },
      error: (error) => {
        console.error('Error al responder solicitud:', error);
        this.notificationService.error(this.errorHandler.getMensaje(error, 'solicitud'));
      }
    });
  }

  get solicitudesFiltradas(): SolicitudDispositivo[] {
    let resultado = this.solicitudes;
    if (this.filtroEstado !== 'TODOS') {
      resultado = resultado.filter(s => s.estado === this.filtroEstado);
    }
    if (this.filtroTipo !== 'TODOS') {
      resultado = resultado.filter(s => s.tipoSolicitud === this.filtroTipo);
    }
    return resultado;
  }

  getEstadoClass(estado: EstadoSolicitudDispositivo): string {
    switch (estado) {
      case EstadoSolicitudDispositivo.PENDIENTE:
        return 'badge-warning';
      case EstadoSolicitudDispositivo.ENVIADO_PETROLERA:
        return 'badge-info';
      case EstadoSolicitudDispositivo.APROBADO:
        return 'badge-success';
      case EstadoSolicitudDispositivo.DENEGADO:
        return 'badge-danger';
      case EstadoSolicitudDispositivo.COMPLETADO:
        return 'badge-secondary';
      default:
        return 'badge-secondary';
    }
  }

  getEstadoTexto(estado: EstadoSolicitudDispositivo): string {
    switch (estado) {
      case EstadoSolicitudDispositivo.PENDIENTE: return 'Pendiente';
      case EstadoSolicitudDispositivo.ENVIADO_PETROLERA: return 'Enviado a Petrolera';
      case EstadoSolicitudDispositivo.APROBADO: return 'Aprobado';
      case EstadoSolicitudDispositivo.DENEGADO: return 'Denegado';
      case EstadoSolicitudDispositivo.COMPLETADO: return 'Completado';
      default: return estado;
    }
  }

  getTipoLabel(tipo: TipoSolicitudDispositivo): string {
    const found = this.tiposSolicitud.find(t => t.value === tipo);
    return found ? found.label : tipo;
  }

  requiresDispositivo(): boolean {
    const tipo = this.nuevaSolicitud.tipoSolicitud;
    return tipo === TipoSolicitudDispositivo.SOLICITUD_CREDITO ||
           tipo === TipoSolicitudDispositivo.BAJA_DISPOSITIVO ||
           tipo === TipoSolicitudDispositivo.CAMBIO_MATRICULA;
  }
}
