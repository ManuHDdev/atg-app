import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { SolicitudTarjetaService } from '../../../services/solicitud-tarjeta.service';
import { SocioService } from '../../../services/socio.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { SolicitudTarjeta, TipoPdfSolicitud, getMotivoDuplicadoLabel } from '../../../models/solicitud-tarjeta.model';
import { Socio } from '../../../models/socio.model';
import { Petrolera } from '../../../models/petrolera.model';
import { EstadoBadge } from '../estado-badge/estado-badge';
import { EmailLogs } from '../email-logs/email-logs';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { ZonaSoltarArchivo } from '../../shared/zona-soltar-archivo/zona-soltar-archivo';

@Component({
  selector: 'app-solicitud-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, EstadoBadge, EmailLogs, ZonaSoltarArchivo],
  templateUrl: './solicitud-detalle.html',
  styleUrl: './solicitud-detalle.css'
})
export class SolicitudDetalle implements OnInit {
  solicitud: SolicitudTarjeta | null = null;
  socio: Socio | null = null;
  petrolera: Petrolera | null = null;

  llegadaForm!: FormGroup;
  entregadaForm!: FormGroup;
  bajaForm!: FormGroup;
  duplicadoForm!: FormGroup;

  loading: boolean = false;
  /** Error de una acción de la propia página: se pinta en la cabecera. */
  error: string | null = null;
  success: string | null = null;

  /**
   * Error de una acción lanzada desde un modal. Va aparte de `error` porque la
   * superposición del modal tapa la cabecera: pintarlo ahí dejaba al operador sin
   * ninguna pista de por qué no pasaba nada.
   */
  errorModal: string | null = null;

  /**
   * El operador ya ha intentado enviar el formulario del modal abierto. Hasta entonces
   * no se marca nada en rojo; después se señalan los campos que faltan.
   */
  intentoGuardar: boolean = false;

  showLlegadaModal: boolean = false;
  showEntregadaModal: boolean = false;
  showBajaModal: boolean = false;
  showDuplicadoModal: boolean = false;
  showRechazoModal: boolean = false;
  motivoRechazo: string = '';

  // Circuito del documento firmado. Cada subida tiene su propio fichero seleccionado y su
  // propia zona de subida: compartirlos haría que elegir el borrador dejara "listo para
  // subir" el escaneado firmado, y al revés.
  ficheroEditable: File | null = null;
  ficheroFirmado: File | null = null;
  procesandoDocumento: boolean = false;

  constructor(
    private fb: FormBuilder,
    private solicitudService: SolicitudTarjetaService,
    private socioService: SocioService,
    private petroleraService: PetroleraService,
    private router: Router,
    private route: ActivatedRoute,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.cargarSolicitud(id);
    }
    this.inicializarFormularios();
  }

  inicializarFormularios(): void {
    this.llegadaForm = this.fb.group({
      fechaLlegadaEstimada: ['', Validators.required],
      numeroContrato: [''],  // Opcional
      observaciones: ['']
    });

    this.entregadaForm = this.fb.group({
      observaciones: ['']
    });

    // Formulario para aprobar BAJA con fecha por defecto = hoy
    const hoy = new Date().toISOString().split('T')[0];
    this.bajaForm = this.fb.group({
      fechaBaja: [hoy, Validators.required],
      observaciones: ['']
    });

    // Formulario para aprobar DUPLICADO con fecha por defecto = hoy
    this.duplicadoForm = this.fb.group({
      fechaRespuesta: [hoy, Validators.required],
      observaciones: ['']
    });
  }

  cargarSolicitud(id: string): void {
    this.loading = true;
    this.error = null;

    this.solicitudService.getById(id).subscribe({
      next: (solicitud) => {
        this.solicitud = solicitud;
        this.cargarDatosRelacionados();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar solicitud:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  cargarDatosRelacionados(): void {
    if (!this.solicitud) return;

    this.socioService.getById(this.solicitud.socioId).subscribe({
      next: (socio) => this.socio = socio,
      error: (err) => console.error('Error al cargar socio:', err)
    });

    this.petroleraService.getById(this.solicitud.petroleraId).subscribe({
      next: (petrolera) => this.petrolera = petrolera,
      error: (err) => console.error('Error al cargar petrolera:', err)
    });
  }

  registrarAprobacionPetrolera(): void {
    if (!this.solicitud?.id) return;

    // Para solicitudes de BAJA, abrir modal para ingresar fecha
    if (this.solicitud.tipo === 'BAJA') {
      this.abrirModalBaja();
      return;
    }

    // Para solicitudes de DUPLICADO, abrir modal para ingresar fecha de respuesta
    if (this.solicitud.tipo === 'DUPLICADO') {
      this.abrirModalDuplicado();
      return;
    }

    // Para los demás tipos, registrar la aprobación directamente
    if (!confirm('¿Confirma que la petrolera ha aprobado esta solicitud?')) return;

    this.loading = true;
    this.solicitudService.aprobarPorPetrolera(this.solicitud.id).subscribe({
      next: () => {
        this.success = 'Aprobación de la petrolera registrada correctamente';
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => {
        console.error('Error al registrar la aprobación de la petrolera:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  registrarDenegacionPetrolera(): void {
    this.motivoRechazo = '';
    this.prepararModal();
    this.showRechazoModal = true;
  }

  confirmarDenegacionPetrolera(): void {
    if (!this.solicitud?.id) return;

    this.intentoGuardar = true;
    if (!this.motivoRechazo.trim()) {
      this.enfocarCampo('motivoRechazo');
      return;
    }

    this.errorModal = null;
    this.loading = true;
    this.solicitudService.denegarPorPetrolera(this.solicitud.id, this.motivoRechazo).subscribe({
      next: () => {
        this.showRechazoModal = false;
        this.success = 'Denegación de la petrolera registrada correctamente';
        this.cargarSolicitud(this.solicitud!.id!);
      },
      // El modal sigue abierto: el motivo escrito se conserva para poder reintentar.
      error: (err) => this.falloEnModal('Error al registrar la denegación de la petrolera:', err)
    });
  }

  abrirModalLlegada(): void {
    this.prepararModal();
    this.showLlegadaModal = true;
  }

  registrarLlegada(): void {
    if (!this.solicitud?.id) return;
    if (this.faltanDatos(this.llegadaForm, 'fechaLlegadaEstimada')) return;

    this.errorModal = null;
    this.loading = true;
    this.solicitudService.registrarLlegada(this.solicitud.id, this.llegadaForm.value).subscribe({
      next: () => {
        this.success = 'Llegada registrada exitosamente';
        this.showLlegadaModal = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.falloEnModal('Error al registrar llegada:', err)
    });
  }

  abrirModalEntregada(): void {
    this.prepararModal();
    this.showEntregadaModal = true;
  }

  marcarEntregada(): void {
    if (!this.solicitud?.id) return;
    if (this.faltanDatos(this.entregadaForm, 'observacionesEntrega')) return;

    this.errorModal = null;
    this.loading = true;
    this.solicitudService.marcarEntregada(this.solicitud.id, this.entregadaForm.value).subscribe({
      next: () => {
        this.success = 'Entrega registrada: la solicitud queda completada';
        this.showEntregadaModal = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.falloEnModal('Error al marcar como entregada:', err)
    });
  }

  // ---------- circuito del documento firmado ----------

  onFicheroEditableSeleccionado(fichero: File): void {
    this.error = null;
    this.ficheroEditable = fichero;
  }

  onFicheroFirmadoSeleccionado(fichero: File): void {
    this.error = null;
    this.ficheroFirmado = fichero;
  }

  /** La zona de subida ya ha validado tipo y tamaño: aquí solo se muestra el motivo. */
  onFicheroRechazado(mensaje: string): void {
    this.error = mensaje;
  }

  guardarPdfEditado(): void {
    if (!this.solicitud?.id || !this.ficheroEditable) return;

    this.procesandoDocumento = true;
    this.solicitudService.guardarPdfEditado(this.solicitud.id, this.ficheroEditable).subscribe({
      next: () => {
        this.success = 'Impreso actualizado correctamente';
        this.limpiarSeleccionEditable();
        this.procesandoDocumento = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.fallo('Error al guardar el impreso editado', err)
    });
  }

  enviarASocio(): void {
    if (!this.solicitud?.id) return;
    if (!confirm('¿Enviar el impreso al socio para que lo firme? El PDF dejará de ser editable.')) return;

    this.procesandoDocumento = true;
    this.solicitudService.enviarASocio(this.solicitud.id).subscribe({
      next: () => {
        this.success = 'Impreso enviado al socio para su firma';
        this.limpiarSeleccionEditable();
        this.procesandoDocumento = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.fallo('Error al enviar el impreso al socio', err)
    });
  }

  subirPdfFirmado(): void {
    if (!this.solicitud?.id || !this.ficheroFirmado) return;

    this.procesandoDocumento = true;
    this.solicitudService.subirPdfFirmado(this.solicitud.id, this.ficheroFirmado).subscribe({
      next: () => {
        this.success = 'Impreso firmado registrado correctamente';
        this.limpiarSeleccionFirmado();
        this.procesandoDocumento = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.fallo('Error al registrar el impreso firmado', err)
    });
  }

  aceptarFirmaSocio(): void {
    if (!this.solicitud?.id) return;
    if (!confirm('¿Dar por buena la firma del socio? La solicitud quedará lista para presentarla a la petrolera.')) return;

    this.procesandoDocumento = true;
    this.solicitudService.aceptarFirmaSocio(this.solicitud.id).subscribe({
      next: () => {
        this.success = 'Firma del socio aceptada';
        this.procesandoDocumento = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.fallo('Error al aceptar la firma del socio', err)
    });
  }

  enviarAPetrolera(): void {
    if (!this.solicitud?.id) return;
    if (!confirm('¿Presentar la solicitud a la petrolera? Se le enviará el impreso firmado junto con los datos del socio.')) return;

    this.procesandoDocumento = true;
    this.solicitudService.enviarAPetrolera(this.solicitud.id).subscribe({
      next: () => {
        this.success = 'Solicitud presentada a la petrolera';
        this.procesandoDocumento = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.fallo('Error al presentar la solicitud a la petrolera', err)
    });
  }

  descargarPdf(tipo: TipoPdfSolicitud): void {
    if (!this.solicitud?.id) return;

    this.solicitudService.descargarPdf(this.solicitud.id, tipo).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = `${this.solicitud?.numeroSolicitud ?? this.solicitud?.id}_${tipo}.pdf`;
        enlace.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Error al descargar el impreso:', err);
        this.error = this.errorHandler.getMensaje(err);
      }
    });
  }

  verPdf(tipo: TipoPdfSolicitud): void {
    if (!this.solicitud?.id) return;

    this.solicitudService.descargarPdf(this.solicitud.id, tipo).subscribe({
      next: (blob) => window.open(window.URL.createObjectURL(blob), '_blank'),
      error: (err) => {
        console.error('Error al abrir el impreso:', err);
        this.error = this.errorHandler.getMensaje(err);
      }
    });
  }

  private limpiarSeleccionEditable(): void {
    this.ficheroEditable = null;
  }

  private limpiarSeleccionFirmado(): void {
    this.ficheroFirmado = null;
  }

  private fallo(mensaje: string, err: unknown): void {
    console.error(mensaje, err);
    this.error = this.errorHandler.getMensaje(err);
    this.procesandoDocumento = false;
  }

  // ---------- validación y errores de los modales ----------

  /**
   * Misma convención que el resto de formularios de la aplicación (`solicitud-form`,
   * `plantilla-form`): el campo se marca en cuanto el operador lo ha tocado o en cuanto
   * ha intentado enviar, lo que ocurra antes.
   */
  campoInvalido(formulario: FormGroup, nombre: string): boolean {
    const control = formulario.get(nombre);
    if (!control) return false;
    return control.invalid && (this.intentoGuardar || control.touched);
  }

  /** El motivo de la denegación se edita con ngModel, así que no tiene control que consultar. */
  get motivoRechazoInvalido(): boolean {
    return this.intentoGuardar && !this.motivoRechazo.trim();
  }

  /**
   * El botón de envío ya no está muerto: si falta algo se marca el campo y se le lleva el
   * foco, para que el operador sepa qué le falta en lugar de adivinarlo.
   */
  private faltanDatos(formulario: FormGroup, idPrimerCampo: string): boolean {
    this.intentoGuardar = true;
    if (formulario.valid) return false;

    formulario.markAllAsTouched();
    this.enfocarCampo(idPrimerCampo);
    return true;
  }

  private enfocarCampo(id: string): void {
    document.getElementById(id)?.focus();
  }

  /** Error de una acción lanzada desde un modal: se muestra dentro del propio modal. */
  private falloEnModal(mensaje: string, err: unknown): void {
    console.error(mensaje, err);
    this.errorModal = this.errorHandler.getMensaje(err);
    this.loading = false;
  }

  /** Cada vez que se abre un modal se parte de cero: ni errores ni campos marcados. */
  private prepararModal(): void {
    this.errorModal = null;
    this.intentoGuardar = false;
  }

  /** Una LLEGADA no lleva papeleo: nunca entra en el circuito del documento firmado. */
  get tieneCircuitoDeFirma(): boolean {
    return !!this.solicitud && this.solicitud.tipo !== 'LLEGADA' && !!this.solicitud.numeroSolicitud;
  }

  get puedeEditarImpreso(): boolean {
    return this.tieneCircuitoDeFirma && this.solicitud?.estado === 'BORRADOR';
  }

  get puedeEnviarASocio(): boolean {
    return this.puedeEditarImpreso && !!this.solicitud?.rutaPdfEditable;
  }

  get puedeSubirFirmado(): boolean {
    return this.tieneCircuitoDeFirma && this.solicitud?.estado === 'ENVIADO_SOCIO';
  }

  get puedeAceptarFirma(): boolean {
    return this.puedeSubirFirmado && !!this.solicitud?.rutaPdfFirmado;
  }

  get puedeEnviarAPetrolera(): boolean {
    return this.tieneCircuitoDeFirma && this.solicitud?.estado === 'FIRMADO_SOCIO';
  }

  // Las acciones dependen del estado Y del tipo: una LLEGADA nace ya registrada, así que
  // nunca hay que pedirle respuesta a la petrolera ni volver a registrar su llegada.
  get puedeRegistrarRespuestaPetrolera(): boolean {
    return this.solicitud?.estado === 'PENDIENTE' && this.solicitud?.tipo !== 'LLEGADA';
  }

  // Un duplicado también es una tarjeta física que llega y hay que entregar, así que recorre
  // llegada y entrega igual que un alta.
  get puedeRegistrarLlegada(): boolean {
    return this.solicitud?.estado === 'APROBADA'
      && (this.solicitud?.tipo === 'ALTA' || this.solicitud?.tipo === 'DUPLICADO');
  }

  get puedeMarcarEntregada(): boolean {
    return this.solicitud?.estado === 'TARJETA_LLEGADA'
      && (this.solicitud?.tipo === 'ALTA' || this.solicitud?.tipo === 'LLEGADA'
        || this.solicitud?.tipo === 'DUPLICADO');
  }

  /** Texto legible del motivo del duplicado ("Deterioro" / "Extravío"). */
  get motivoDuplicadoTexto(): string {
    return getMotivoDuplicadoLabel(this.solicitud?.motivoDuplicado);
  }

  formatearFecha(fecha: Date | string | undefined): string {
    if (!fecha) return '-';
    return new Date(fecha).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  volver(): void {
    this.router.navigate(['/solicitudes-tarjetas/dashboard']);
  }

  abrirModalBaja(): void {
    // Resetear fecha a hoy cada vez que se abre el modal
    const hoy = new Date().toISOString().split('T')[0];
    this.bajaForm.patchValue({ fechaBaja: hoy });
    this.prepararModal();
    this.showBajaModal = true;
  }

  registrarAprobacionBajaPetrolera(): void {
    if (!this.solicitud?.id) return;
    if (this.faltanDatos(this.bajaForm, 'fechaBaja')) return;

    this.errorModal = null;
    this.loading = true;
    this.solicitudService.aprobarBajaPorPetrolera(this.solicitud.id, this.bajaForm.value).subscribe({
      next: () => {
        this.success = 'Baja aprobada por la petrolera. La tarjeta ha sido dada de baja.';
        this.showBajaModal = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.falloEnModal('Error al aprobar BAJA:', err)
    });
  }

  abrirModalDuplicado(): void {
    // Resetear fecha a hoy cada vez que se abre el modal
    const hoy = new Date().toISOString().split('T')[0];
    this.duplicadoForm.patchValue({ fechaRespuesta: hoy });
    this.prepararModal();
    this.showDuplicadoModal = true;
  }

  registrarAprobacionDuplicadoPetrolera(): void {
    if (!this.solicitud?.id) return;
    if (this.faltanDatos(this.duplicadoForm, 'fechaRespuesta')) return;

    this.errorModal = null;
    this.loading = true;
    this.solicitudService.aprobarDuplicadoPorPetrolera(this.solicitud.id, this.duplicadoForm.value).subscribe({
      next: () => {
        this.success = 'Duplicado aprobado por la petrolera. Queda pendiente de registrar su llegada y la entrega al socio.';
        this.showDuplicadoModal = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => this.falloEnModal('Error al aprobar DUPLICADO:', err)
    });
  }

  cerrarModal(): void {
    this.showLlegadaModal = false;
    this.showEntregadaModal = false;
    this.showBajaModal = false;
    this.showDuplicadoModal = false;
    this.showRechazoModal = false;
    this.errorModal = null;
    this.intentoGuardar = false;
  }
}
