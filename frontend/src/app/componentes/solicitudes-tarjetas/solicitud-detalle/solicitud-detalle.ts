import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { SolicitudTarjetaService } from '../../../services/solicitud-tarjeta.service';
import { SocioService } from '../../../services/socio.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { SolicitudTarjeta } from '../../../models/solicitud-tarjeta.model';
import { Socio } from '../../../models/socio.model';
import { Petrolera } from '../../../models/petrolera.model';
import { EstadoBadge } from '../estado-badge/estado-badge';
import { EmailLogs } from '../email-logs/email-logs';
import { ErrorHandlerService } from '../../../services/error-handler.service';

@Component({
  selector: 'app-solicitud-detalle',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, EstadoBadge, EmailLogs],
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
  error: string | null = null;
  success: string | null = null;

  showLlegadaModal: boolean = false;
  showEntregadaModal: boolean = false;
  showBajaModal: boolean = false;
  showDuplicadoModal: boolean = false;
  showRechazoModal: boolean = false;
  motivoRechazo: string = '';

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
      observaciones: [''],
      procesadoPor: ['Admin', Validators.required]
    });

    this.entregadaForm = this.fb.group({
      observaciones: [''],
      procesadoPor: ['Admin', Validators.required]
    });

    // Formulario para aprobar BAJA con fecha por defecto = hoy
    const hoy = new Date().toISOString().split('T')[0];
    this.bajaForm = this.fb.group({
      fechaBaja: [hoy, Validators.required],
      observaciones: [''],
      procesadoPor: ['Admin', Validators.required]
    });

    // Formulario para aprobar DUPLICADO con fecha por defecto = hoy
    this.duplicadoForm = this.fb.group({
      fechaRespuesta: [hoy, Validators.required],
      observaciones: [''],
      procesadoPor: ['Admin', Validators.required]
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

  aprobar(): void {
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

    // Para otros tipos, aprobar directamente
    if (!confirm('¿Está seguro de aprobar esta solicitud?')) return;

    this.loading = true;
    this.solicitudService.aprobar(this.solicitud.id, 'Admin').subscribe({
      next: () => {
        this.success = 'Solicitud aprobada exitosamente';
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => {
        console.error('Error al aprobar:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  rechazar(): void {
    this.motivoRechazo = '';
    this.showRechazoModal = true;
  }

  confirmarRechazo(): void {
    if (!this.solicitud?.id || !this.motivoRechazo.trim()) return;

    this.showRechazoModal = false;
    this.loading = true;
    this.solicitudService.rechazar(this.solicitud.id, this.motivoRechazo, 'Admin').subscribe({
      next: () => {
        this.success = 'Solicitud rechazada';
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => {
        console.error('Error al rechazar:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  abrirModalLlegada(): void {
    this.showLlegadaModal = true;
  }

  registrarLlegada(): void {
    if (this.llegadaForm.invalid || !this.solicitud?.id) return;

    this.loading = true;
    this.solicitudService.registrarLlegada(this.solicitud.id, this.llegadaForm.value).subscribe({
      next: () => {
        this.success = 'Llegada registrada exitosamente';
        this.showLlegadaModal = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => {
        console.error('Error al registrar llegada:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  abrirModalEntregada(): void {
    this.showEntregadaModal = true;
  }

  marcarEntregada(): void {
    if (this.entregadaForm.invalid || !this.solicitud?.id) return;

    this.loading = true;
    this.solicitudService.marcarEntregada(this.solicitud.id, this.entregadaForm.value).subscribe({
      next: () => {
        this.success = 'Solicitud marcada como entregada exitosamente';
        this.showEntregadaModal = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => {
        console.error('Error al marcar como entregada:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  finalizar(): void {
    if (!this.solicitud?.id || !confirm('¿Está seguro de finalizar esta solicitud?')) return;

    this.loading = true;
    this.solicitudService.finalizar(this.solicitud.id).subscribe({
      next: () => {
        this.success = 'Solicitud finalizada exitosamente';
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => {
        console.error('Error al finalizar:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  get puedeAprobar(): boolean {
    return this.solicitud?.estado === 'PENDIENTE';
  }

  get puedeRegistrarLlegada(): boolean {
    return this.solicitud?.estado === 'APROBADA';
  }

  get puedeMarcarEntregada(): boolean {
    return this.solicitud?.estado === 'TARJETA_LLEGADA';
  }

  get puedeFinalizar(): boolean {
    return this.solicitud?.estado === 'ENTREGADA';
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
    this.showBajaModal = true;
  }

  aprobarBaja(): void {
    if (this.bajaForm.invalid || !this.solicitud?.id) return;

    this.loading = true;
    this.solicitudService.aprobarBaja(this.solicitud.id, this.bajaForm.value).subscribe({
      next: () => {
        this.success = 'Solicitud de BAJA aprobada exitosamente. La tarjeta ha sido dada de baja.';
        this.showBajaModal = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => {
        console.error('Error al aprobar BAJA:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  abrirModalDuplicado(): void {
    // Resetear fecha a hoy cada vez que se abre el modal
    const hoy = new Date().toISOString().split('T')[0];
    this.duplicadoForm.patchValue({ fechaRespuesta: hoy });
    this.showDuplicadoModal = true;
  }

  aprobarDuplicado(): void {
    if (this.duplicadoForm.invalid || !this.solicitud?.id) return;

    this.loading = true;
    this.solicitudService.aprobarDuplicado(this.solicitud.id, this.duplicadoForm.value).subscribe({
      next: () => {
        this.success = 'Solicitud de DUPLICADO aprobada exitosamente. Se ha incrementado la cantidad de tarjetas.';
        this.showDuplicadoModal = false;
        this.cargarSolicitud(this.solicitud!.id!);
      },
      error: (err) => {
        console.error('Error al aprobar DUPLICADO:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  cerrarModal(): void {
    this.showLlegadaModal = false;
    this.showEntregadaModal = false;
    this.showBajaModal = false;
    this.showDuplicadoModal = false;
    this.showRechazoModal = false;
  }
}
