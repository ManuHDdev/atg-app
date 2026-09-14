import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { SolicitudTarjetaService } from '../../services/solicitud-tarjeta.service';
import { SocioService } from '../../services/socio.service';
import { PetroleraService } from '../../services/petrolera.service';
import { MotivoDuplicado, SolicitudTarjeta, getMotivoDuplicadoLabel } from '../../models/solicitud-tarjeta.model';
import { Socio } from '../../models/socio.model';
import { Petrolera } from '../../models/petrolera.model';
import { EstadoBadge } from './estado-badge/estado-badge';
import { ErrorHandlerService } from '../../services/error-handler.service';

@Component({
  selector: 'app-solicitudes-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, EstadoBadge],
  templateUrl: './solicitudes-dashboard.html',
  styleUrl: './solicitudes-dashboard.css'
})
export class SolicitudesDashboard implements OnInit {
  solicitudes: SolicitudTarjeta[] = [];
  solicitudesRecientes: SolicitudTarjeta[] = [];

  totalPendientes: number = 0;
  completadasHoy: number = 0;
  totalCompletadas: number = 0;
  totalRechazadas: number = 0;

  loading: boolean = false;
  error: string | null = null;

  // Modal detalle
  solicitudDetalle?: SolicitudTarjeta;
  socioDetalle?: Socio;
  petroleraDetalle?: Petrolera;
  cargandoDetalle: boolean = false;

  // Sub-modal rechazo
  mostrarModalRechazo: boolean = false;
  motivoRechazo: string = '';

  // Sub-modal BAJA
  mostrarModalBaja: boolean = false;
  fechaBaja: string = '';
  observacionesBaja: string = '';

  // Sub-modal DUPLICADO
  mostrarModalDuplicado: boolean = false;
  fechaDuplicado: string = '';
  observacionesDuplicado: string = '';

  loadingAccion: boolean = false;

  constructor(
    private solicitudService: SolicitudTarjetaService,
    private socioService: SocioService,
    private petroleraService: PetroleraService,
    private router: Router,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading = true;
    this.error = null;

    this.solicitudService.getAll().subscribe({
      next: (data) => {
        this.solicitudes = data;
        this.calcularEstadisticas();
        this.solicitudesRecientes = this.solicitudes
          .sort((a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime())
          .slice(0, 10);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar solicitudes:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  calcularEstadisticas(): void {
    this.totalPendientes = this.solicitudes.filter(s => s.estado === 'PENDIENTE').length;
    this.totalCompletadas = this.solicitudes.filter(s => s.estado === 'COMPLETADA').length;
    this.totalRechazadas = this.solicitudes.filter(s => s.estado === 'RECHAZADA').length;

    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    this.completadasHoy = this.solicitudes.filter(s => {
      if (s.estado === 'COMPLETADA' && s.fechaProcesado) {
        const fecha = new Date(s.fechaProcesado);
        fecha.setHours(0, 0, 0, 0);
        return fecha.getTime() === hoy.getTime();
      }
      return false;
    }).length;
  }

  navegarAFormulario(tipo: string): void {
    this.router.navigate(['/solicitudes-tarjetas', tipo.toLowerCase()]);
  }

  verTodasSolicitudes(): void {
    this.router.navigate(['/solicitudes-tarjetas']);
  }

  verPlantillas(): void {
    this.router.navigate(['/plantillas-tarjetas']);
  }

  verDetalle(id: string): void {
    this.router.navigate(['/solicitudes-tarjetas/detalle', id]);
  }

  verDetalleModal(solicitud: SolicitudTarjeta): void {
    this.solicitudDetalle = solicitud;
    this.socioDetalle = undefined;
    this.petroleraDetalle = undefined;
    this.cargandoDetalle = true;

    Promise.all([
      this.socioService.getById(solicitud.socioId).toPromise(),
      this.petroleraService.getById(solicitud.petroleraId).toPromise()
    ]).then(([socio, petrolera]) => {
      this.socioDetalle = socio;
      this.petroleraDetalle = petrolera;
      this.cargandoDetalle = false;
    }).catch(() => {
      this.cargandoDetalle = false;
    });
  }

  cerrarDetalle(): void {
    this.solicitudDetalle = undefined;
    this.socioDetalle = undefined;
    this.petroleraDetalle = undefined;
    this.mostrarModalRechazo = false;
    this.mostrarModalBaja = false;
    this.mostrarModalDuplicado = false;
  }

  // Una LLEGADA nace ya registrada (TARJETA_LLEGADA): nunca hay respuesta de la petrolera
  // que registrar sobre ella.
  get puedeRegistrarRespuestaPetrolera(): boolean {
    return this.solicitudDetalle?.estado === 'PENDIENTE' && this.solicitudDetalle?.tipo !== 'LLEGADA';
  }

  registrarAprobacionPetrolera(): void {
    if (!this.solicitudDetalle) return;

    if (this.solicitudDetalle.tipo === 'BAJA') {
      const hoy = new Date().toISOString().split('T')[0];
      this.fechaBaja = hoy;
      this.observacionesBaja = '';
      this.mostrarModalBaja = true;
      return;
    }

    if (this.solicitudDetalle.tipo === 'DUPLICADO') {
      const hoy = new Date().toISOString().split('T')[0];
      this.fechaDuplicado = hoy;
      this.observacionesDuplicado = '';
      this.mostrarModalDuplicado = true;
      return;
    }

    // ALTA u otros: registrar la aprobación directamente
    this.loadingAccion = true;
    this.solicitudService.aprobarPorPetrolera(this.solicitudDetalle.id!).subscribe({
      next: () => {
        this.loadingAccion = false;
        this.cerrarDetalle();
        this.cargarDatos();
      },
      error: (err) => {
        console.error('Error al registrar la aprobación de la petrolera:', err);
        this.loadingAccion = false;
      }
    });
  }

  abrirModalDenegacion(): void {
    this.motivoRechazo = '';
    this.mostrarModalRechazo = true;
  }

  confirmarDenegacionPetrolera(): void {
    if (!this.solicitudDetalle?.id || !this.motivoRechazo.trim()) return;

    this.loadingAccion = true;
    this.solicitudService.denegarPorPetrolera(this.solicitudDetalle.id, this.motivoRechazo).subscribe({
      next: () => {
        this.loadingAccion = false;
        this.cerrarDetalle();
        this.cargarDatos();
      },
      error: (err) => {
        console.error('Error al registrar la denegación de la petrolera:', err);
        this.loadingAccion = false;
      }
    });
  }

  confirmarBaja(): void {
    if (!this.solicitudDetalle?.id || !this.fechaBaja) return;

    this.loadingAccion = true;
    this.solicitudService.aprobarBajaPorPetrolera(this.solicitudDetalle.id, {
      fechaBaja: this.fechaBaja,
      observaciones: this.observacionesBaja
    }).subscribe({
      next: () => {
        this.loadingAccion = false;
        this.cerrarDetalle();
        this.cargarDatos();
      },
      error: (err) => {
        console.error('Error al registrar la aprobación de BAJA por la petrolera:', err);
        this.loadingAccion = false;
      }
    });
  }

  confirmarDuplicado(): void {
    if (!this.solicitudDetalle?.id || !this.fechaDuplicado) return;

    this.loadingAccion = true;
    this.solicitudService.aprobarDuplicadoPorPetrolera(this.solicitudDetalle.id, {
      fechaRespuesta: this.fechaDuplicado,
      observaciones: this.observacionesDuplicado
    }).subscribe({
      next: () => {
        this.loadingAccion = false;
        this.cerrarDetalle();
        this.cargarDatos();
      },
      error: (err) => {
        console.error('Error al registrar la aprobación de DUPLICADO por la petrolera:', err);
        this.loadingAccion = false;
      }
    });
  }

  getEstadoBadgeClass(estado: string): string {
    switch (estado) {
      case 'PENDIENTE': return 'badge-warning';
      case 'COMPLETADA': return 'badge-success';
      case 'RECHAZADA': return 'badge-danger';
      default: return 'badge-secondary';
    }
  }

  /** Texto legible del motivo del duplicado ("Deterioro" / "Extravío"). */
  getMotivoDuplicado(motivo: MotivoDuplicado | undefined): string {
    return getMotivoDuplicadoLabel(motivo);
  }

  getTipoBadgeClass(tipo: string): string {
    switch (tipo) {
      case 'LLEGADA': return 'badge-info';
      case 'ALTA': return 'badge-primary';
      case 'BAJA': return 'badge-danger';
      case 'DUPLICADO': return 'badge-warning';
      default: return 'badge-secondary';
    }
  }

  formatearFecha(fecha: Date | undefined): string {
    if (!fecha) return '-';
    return new Date(fecha).toLocaleDateString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
