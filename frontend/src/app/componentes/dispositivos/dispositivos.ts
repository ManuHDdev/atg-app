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
  EstadoSolicitudDispositivo,
  TipoPdfSolicitudDispositivo
} from '../../models/dispositivo.model';
import { Socio } from '../../models/socio.model';
import { Empresa } from '../../models/empresa.model';
import { Petrolera, petroleraPermite } from '../../models/petrolera.model';
import { EmailLogs } from '../solicitudes-tarjetas/email-logs/email-logs';
import { SocioAutocomplete } from '../shared/socio-autocomplete/socio-autocomplete';
import { ZonaSoltarArchivo } from '../shared/zona-soltar-archivo/zona-soltar-archivo';

@Component({
  selector: 'app-dispositivos',
  standalone: true,
  imports: [CommonModule, FormsModule, EmailLogs, SocioAutocomplete, ZonaSoltarArchivo],
  templateUrl: './dispositivos.html',
  styleUrl: './dispositivos.css',
})
export class Dispositivos implements OnInit {
  solicitudes: SolicitudDispositivo[] = [];
  socios: Socio[] = [];
  empresas: Empresa[] = [];
  /** Petroleras activas que operan con dispositivos (null = sin restricción). */
  petrolerasDispositivos: Petrolera[] = [];
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

  // Circuito del documento firmado. Cada subida tiene su propio fichero seleccionado y su
  // propia zona de subida: compartirlos haría que elegir el borrador dejara "listo para subir" el
  // escaneado firmado, y al revés.
  ficheroEditable: File | null = null;
  ficheroFirmado: File | null = null;
  procesandoDocumento = false;

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
  /** Importe concedido por la petrolera; se precarga con el solicitado al abrir el modal. */
  montoConcedidoRespuesta: number | null = null;

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
        this.petrolerasDispositivos = data.filter(p => p.activa && petroleraPermite(p.operaDispositivos));
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

  /**
   * Petroleras ofrecidas en el desplegable del formulario.
   * Para "Solicitud de Crédito" el procedimiento restringe aún más la lista
   * (flag permiteCreditoDispositivo).
   */
  get petroleras(): Petrolera[] {
    if (this.nuevaSolicitud.tipoSolicitud === TipoSolicitudDispositivo.SOLICITUD_CREDITO) {
      return this.petrolerasDispositivos.filter(p => petroleraPermite(p.permiteCreditoDispositivo));
    }
    return this.petrolerasDispositivos;
  }

  onTipoSolicitudChange(): void {
    // Limpiar campos condicionales al cambiar tipo
    this.nuevaSolicitud.dispositivoId = undefined;
    this.nuevaSolicitud.matricula = undefined;
    this.nuevaSolicitud.matriculaDestino = undefined;
    this.nuevaSolicitud.monto = undefined;

    // La petrolera ya elegida puede no admitir el nuevo tipo: no dejar una combinación imposible
    const petroleraId = Number(this.nuevaSolicitud.petroleraId);
    if (petroleraId && !this.petroleras.some(p => p.id === petroleraId)) {
      this.nuevaSolicitud.petroleraId = 0;
      this.notificationService.warning(
        'La petrolera seleccionada no admite este tipo de solicitud. Seleccione otra petrolera.'
      );
    }
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
    this.limpiarSeleccionEditable();
    this.limpiarSeleccionFirmado();
  }

  cerrarDetalle(): void {
    this.solicitudSeleccionada = undefined;
    this.limpiarSeleccionEditable();
    this.limpiarSeleccionFirmado();
  }

  enviarAPetrolera(solicitud: SolicitudDispositivo): void {
    if (!confirm('¿Presentar la solicitud a la petrolera? Se le enviará el impreso firmado junto con los datos del socio.')) return;

    this.procesandoDocumento = true;
    this.dispositivoService.enviarAPetrolera(solicitud.id!).subscribe({
      next: () => {
        this.notificationService.success('Solicitud presentada a la petrolera');
        this.procesandoDocumento = false;
        this.refrescarTrasTransicion(solicitud.id!);
      },
      error: (error) => this.falloDocumento('Error al presentar la solicitud a la petrolera', error)
    });
  }

  // ---------- circuito del documento firmado ----------

  onFicheroEditableSeleccionado(fichero: File): void {
    this.ficheroEditable = fichero;
  }

  onFicheroFirmadoSeleccionado(fichero: File): void {
    this.ficheroFirmado = fichero;
  }

  /** La zona de subida ya ha validado tipo y tamaño: aquí solo se avisa del motivo. */
  onFicheroRechazado(mensaje: string): void {
    this.notificationService.error(mensaje);
  }

  guardarPdfEditado(): void {
    if (!this.solicitudSeleccionada?.id || !this.ficheroEditable) return;

    const id = this.solicitudSeleccionada.id;
    this.procesandoDocumento = true;
    this.dispositivoService.guardarPdfEditado(id, this.ficheroEditable).subscribe({
      next: () => {
        this.notificationService.success('Impreso actualizado correctamente');
        this.limpiarSeleccionEditable();
        this.procesandoDocumento = false;
        this.refrescarTrasTransicion(id);
      },
      error: (error) => this.falloDocumento('Error al guardar el impreso editado', error)
    });
  }

  enviarASocio(): void {
    if (!this.solicitudSeleccionada?.id) return;
    if (!confirm('¿Enviar el impreso al socio para que lo firme? El PDF dejará de ser editable.')) return;

    const id = this.solicitudSeleccionada.id;
    this.procesandoDocumento = true;
    this.dispositivoService.enviarASocio(id).subscribe({
      next: () => {
        this.notificationService.success('Impreso enviado al socio para su firma');
        this.limpiarSeleccionEditable();
        this.procesandoDocumento = false;
        this.refrescarTrasTransicion(id);
      },
      error: (error) => this.falloDocumento('Error al enviar el impreso al socio', error)
    });
  }

  subirPdfFirmado(): void {
    if (!this.solicitudSeleccionada?.id || !this.ficheroFirmado) return;

    const id = this.solicitudSeleccionada.id;
    this.procesandoDocumento = true;
    this.dispositivoService.subirPdfFirmado(id, this.ficheroFirmado).subscribe({
      next: () => {
        this.notificationService.success('Impreso firmado registrado correctamente');
        this.limpiarSeleccionFirmado();
        this.procesandoDocumento = false;
        this.refrescarTrasTransicion(id);
      },
      error: (error) => this.falloDocumento('Error al registrar el impreso firmado', error)
    });
  }

  aceptarFirmaSocio(): void {
    if (!this.solicitudSeleccionada?.id) return;
    if (!confirm('¿Dar por buena la firma del socio? La solicitud quedará lista para presentarla a la petrolera.')) return;

    const id = this.solicitudSeleccionada.id;
    this.procesandoDocumento = true;
    this.dispositivoService.aceptarFirmaSocio(id).subscribe({
      next: () => {
        this.notificationService.success('Firma del socio aceptada');
        this.procesandoDocumento = false;
        this.refrescarTrasTransicion(id);
      },
      error: (error) => this.falloDocumento('Error al aceptar la firma del socio', error)
    });
  }

  presentarAPetrolera(): void {
    if (!this.solicitudSeleccionada) return;
    this.enviarAPetrolera(this.solicitudSeleccionada);
  }

  verPdf(tipo: TipoPdfSolicitudDispositivo): void {
    if (!this.solicitudSeleccionada?.id) return;

    this.dispositivoService.descargarPdf(this.solicitudSeleccionada.id, tipo).subscribe({
      next: (blob) => window.open(window.URL.createObjectURL(blob), '_blank'),
      error: (error) => {
        console.error('Error al abrir el impreso:', error);
        this.notificationService.error(this.errorHandler.getMensaje(error, 'solicitud'));
      }
    });
  }

  descargarPdf(tipo: TipoPdfSolicitudDispositivo): void {
    if (!this.solicitudSeleccionada?.id) return;

    const solicitud = this.solicitudSeleccionada;
    this.dispositivoService.descargarPdf(solicitud.id!, tipo).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `${solicitud.numeroSolicitud ?? solicitud.id}_${tipo}.pdf`;
        enlace.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error al descargar el impreso:', error);
        this.notificationService.error(this.errorHandler.getMensaje(error, 'solicitud'));
      }
    });
  }

  /** Refresca el listado y el detalle abierto tras avanzar una etapa del circuito. */
  private refrescarTrasTransicion(id: number): void {
    this.cargarSolicitudes();
    this.dispositivoService.obtenerSolicitudPorId(id).subscribe({
      next: (solicitud) => this.solicitudSeleccionada = solicitud,
      error: (error) => console.error('Error al recargar la solicitud:', error)
    });
  }

  private limpiarSeleccionEditable(): void {
    this.ficheroEditable = null;
  }

  private limpiarSeleccionFirmado(): void {
    this.ficheroFirmado = null;
  }

  private falloDocumento(mensaje: string, error: unknown): void {
    console.error(mensaje, error);
    this.notificationService.error(this.errorHandler.getMensaje(error, 'solicitud'));
    this.procesandoDocumento = false;
  }

  /**
   * Las solicitudes anteriores al circuito de firma no tienen número y por tanto no tienen
   * impresos asociados: su documentación se tramita fuera del sistema.
   */
  get tieneCircuitoDeFirma(): boolean {
    return !!this.solicitudSeleccionada?.numeroSolicitud;
  }

  get puedeEditarImpreso(): boolean {
    return this.tieneCircuitoDeFirma
      && this.solicitudSeleccionada?.estado === EstadoSolicitudDispositivo.BORRADOR;
  }

  get puedeEnviarASocio(): boolean {
    return this.puedeEditarImpreso && !!this.solicitudSeleccionada?.rutaPdfEditable;
  }

  get puedeSubirFirmado(): boolean {
    return this.tieneCircuitoDeFirma
      && this.solicitudSeleccionada?.estado === EstadoSolicitudDispositivo.ENVIADO_SOCIO;
  }

  get puedeAceptarFirma(): boolean {
    return this.puedeSubirFirmado && !!this.solicitudSeleccionada?.rutaPdfFirmado;
  }

  get puedeEnviarAPetrolera(): boolean {
    return this.tieneCircuitoDeFirma
      && this.solicitudSeleccionada?.estado === EstadoSolicitudDispositivo.FIRMADO_SOCIO;
  }

  /** Solo cabe registrar la respuesta de la petrolera cuando ya se le ha presentado. */
  get puedeRegistrarRespuestaPetrolera(): boolean {
    return this.solicitudSeleccionada?.estado === EstadoSolicitudDispositivo.ENVIADO_PETROLERA;
  }

  /**
   * Una solicitud heredada (estado PENDIENTE, sin número) nunca entró en el circuito de
   * firma: se presenta a la petrolera como se hacía antes, sin impreso adjunto.
   */
  esSolicitudHeredada(solicitud: SolicitudDispositivo): boolean {
    return solicitud.estado === EstadoSolicitudDispositivo.PENDIENTE && !solicitud.numeroSolicitud;
  }

  abrirModalRespuesta(solicitud: SolicitudDispositivo, aprobado: boolean): void {
    this.solicitudRespondiendo = solicitud;
    this.aprobandoRespuesta = aprobado;
    this.comentarioRespuesta = '';
    this.intentoGuardar = false;
    // Lo habitual es que la petrolera conceda lo solicitado: se precarga para no reescribirlo
    this.montoConcedidoRespuesta = aprobado ? solicitud.monto ?? null : null;
    this.mostrarModalRespuesta = true;
  }

  cerrarModalRespuesta(): void {
    this.mostrarModalRespuesta = false;
    this.solicitudRespondiendo = undefined;
    this.comentarioRespuesta = '';
    this.montoConcedidoRespuesta = null;
    this.intentoGuardar = false;
  }

  /** Solo la solicitud de crédito lleva importe, y por tanto importe concedido. */
  requiereImporteConcedido(solicitud?: SolicitudDispositivo): boolean {
    return !!solicitud && solicitud.tipoSolicitud === TipoSolicitudDispositivo.SOLICITUD_CREDITO;
  }

  /** El importe concedido difiere del solicitado (se resalta en el listado y el detalle). */
  importeDifiere(solicitud: SolicitudDispositivo): boolean {
    return solicitud.montoConcedido != null && solicitud.monto != null
      && Number(solicitud.montoConcedido) !== Number(solicitud.monto);
  }

  confirmarRespuesta(): void {
    if (!this.solicitudRespondiendo) return;
    this.intentoGuardar = true;

    const exigeImporte = this.aprobandoRespuesta && this.requiereImporteConcedido(this.solicitudRespondiendo);
    if (exigeImporte && (!this.montoConcedidoRespuesta || this.montoConcedidoRespuesta <= 0)) {
      this.notificationService.error('El importe concedido es obligatorio y debe ser mayor que 0');
      return;
    }

    if (!confirm('¿Está seguro de ' + (this.aprobandoRespuesta ? 'aprobar' : 'denegar') + ' esta solicitud?')) return;
    this.dispositivoService.responderPetrolera(
      this.solicitudRespondiendo.id!,
      this.aprobandoRespuesta,
      this.comentarioRespuesta,
      exigeImporte ? this.montoConcedidoRespuesta : null
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
      case EstadoSolicitudDispositivo.BORRADOR:
        return 'badge-secondary';
      case EstadoSolicitudDispositivo.ENVIADO_SOCIO:
        return 'badge-info';
      case EstadoSolicitudDispositivo.FIRMADO_SOCIO:
        return 'badge-info';
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
      case EstadoSolicitudDispositivo.BORRADOR: return 'Borrador';
      case EstadoSolicitudDispositivo.ENVIADO_SOCIO: return 'Enviado al Socio';
      case EstadoSolicitudDispositivo.FIRMADO_SOCIO: return 'Firmado por el Socio';
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

  formatearFecha(fecha: string | undefined): string {
    if (!fecha) return '-';
    return new Date(fecha).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  requiresDispositivo(): boolean {
    const tipo = this.nuevaSolicitud.tipoSolicitud;
    return tipo === TipoSolicitudDispositivo.SOLICITUD_CREDITO ||
           tipo === TipoSolicitudDispositivo.BAJA_DISPOSITIVO ||
           tipo === TipoSolicitudDispositivo.CAMBIO_MATRICULA;
  }
}
