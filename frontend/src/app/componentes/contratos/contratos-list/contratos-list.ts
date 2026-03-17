import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { SolicitudContratoService } from '../../../services/solicitud-contrato.service';
import { TipoContratoService } from '../../../services/tipo-contrato.service';
import { TipoSolicitudService } from '../../../services/tipo-solicitud.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { SocioService } from '../../../services/socio.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { SolicitudContrato, EstadoSolicitud, TipoSolicitudContrato, FiltroSolicitudesDTO } from '../../../models/solicitud-contrato.model';
import { TipoContrato } from '../../../models/tipo-contrato.model';
import { TipoSolicitud } from '../../../models/tipo-solicitud.model';
import { Petrolera } from '../../../models/petrolera.model';
import { Socio } from '../../../models/socio.model';

@Component({
  selector: 'app-contratos-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './contratos-list.html',
  styleUrl: './contratos-list.css'
})
export class ContratosList implements OnInit {
  solicitudes: SolicitudContrato[] = [];
  tiposContrato: TipoContrato[] = [];
  tiposSolicitud: TipoSolicitud[] = [];
  petroleras: Petrolera[] = [];
  socios: Socio[] = [];
  loading = false;

  // Paginación
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 10;

  // Filtros
  filtros: FiltroSolicitudesDTO = {
    page: 0,
    size: 10,
    sortBy: 'fechaCreacion',
    sortDirection: 'DESC'
  };

  EstadoSolicitud = EstadoSolicitud;

  constructor(
    private solicitudService: SolicitudContratoService,
    private tipoContratoService: TipoContratoService,
    private tipoSolicitudService: TipoSolicitudService,
    private petroleraService: PetroleraService,
    private socioService: SocioService,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarSolicitudes();
    this.cargarDatosAuxiliares();
  }

  cargarSolicitudes(): void {
    this.loading = true;
    this.solicitudService.listar(this.filtros).subscribe({
      next: (response) => {
        this.solicitudes = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.loading = false;
      },
      error: (err) => {
        this.notificationService.error(this.errorHandler.getMensaje(err));
        this.loading = false;
        console.error(err);
      }
    });
  }

  cargarDatosAuxiliares(): void {
    this.tipoContratoService.getActivos().subscribe({
      next: (tipos) => {
        this.tiposContrato = tipos;
      },
      error: (err) => console.error('Error al cargar tipos de contrato', err)
    });

    this.tipoSolicitudService.getAll().subscribe({
      next: (tipos) => {
        this.tiposSolicitud = tipos;
      },
      error: (err) => console.error('Error al cargar tipos de solicitud', err)
    });

    this.petroleraService.getAll().subscribe({
      next: (petroleras) => {
        this.petroleras = petroleras.filter(p => p.activa);
      },
      error: (err) => console.error('Error al cargar petroleras', err)
    });

    this.socioService.getAll().subscribe({
      next: (socios) => {
        this.socios = socios.filter(s => s.activo);
      },
      error: (err) => console.error('Error al cargar socios', err)
    });
  }

  aplicarFiltros(): void {
    this.filtros.page = 0;
    this.cargarSolicitudes();
  }

  limpiarFiltros(): void {
    this.filtros = {
      page: 0,
      size: 10,
      sortBy: 'fechaCreacion',
      sortDirection: 'DESC'
    };
    this.cargarSolicitudes();
  }

  cambiarPagina(page: number): void {
    this.filtros.page = page;
    this.cargarSolicitudes();
  }

  obtenerNombrePetrolera(petroleraId: number): string {
    const petrolera = this.petroleras.find(p => p.id === petroleraId);
    return petrolera ? petrolera.nombre : 'Desconocida';
  }

  obtenerNombreTipoContrato(tipoContratoId: number): string {
    if (!tipoContratoId) return '-';
    const tipo = this.tiposContrato.find(t => t.id === tipoContratoId);
    return tipo ? tipo.nombre : '-';
  }

  obtenerNombreTipoSolicitudPetrolera(tipoSolicitudId?: number): string {
    if (!tipoSolicitudId) return '-';
    const tipo = this.tiposSolicitud.find(t => Number(t.id) === tipoSolicitudId);
    return tipo ? tipo.nombre : '-';
  }

  obtenerPetroleraConSubtipo(solicitud: SolicitudContrato): string {
    const petrolera = this.obtenerNombrePetrolera(solicitud.petroleraId);
    // Usar subtipoNombre (disponible para todos los tipos: NUEVO, BAJA, CAMBIO_CONDICIONES)
    if (solicitud.subtipoNombre) {
      return `${petrolera} - ${solicitud.subtipoNombre}`;
    }
    // Fallback: resolver desde tipoSolicitudPetroleraId (solo para solicitudes antiguas sin subtipoNombre)
    if (solicitud.tipoSolicitudPetroleraId) {
      const subtipo = this.obtenerNombreTipoSolicitudPetrolera(solicitud.tipoSolicitudPetroleraId);
      if (subtipo && subtipo !== '-') {
        return `${petrolera} - ${subtipo}`;
      }
    }
    return petrolera;
  }

  obtenerNombreSocio(socioId: number): string {
    const socio = this.socios.find(s => Number(s.id) === socioId);
    return socio ? socio.nombre : 'Desconocido';
  }

  getEstadoClass(estado: EstadoSolicitud): string {
    const clases: Record<EstadoSolicitud, string> = {
      [EstadoSolicitud.BORRADOR]: 'badge-warning',
      [EstadoSolicitud.ENVIADO_SOCIO]: 'badge-info',
      [EstadoSolicitud.FIRMADO_SOCIO]: 'badge-primary',
      [EstadoSolicitud.ENVIADO_PETROLERA]: 'badge-warning',
      [EstadoSolicitud.ACEPTADA_PETROLERA]: 'badge-success',
      [EstadoSolicitud.RECHAZADA_PETROLERA]: 'badge-danger'
    };
    return clases[estado] || 'badge-secondary';
  }

  getEstadoTexto(estado: EstadoSolicitud): string {
    const textos: Record<EstadoSolicitud, string> = {
      [EstadoSolicitud.BORRADOR]: 'Borrador',
      [EstadoSolicitud.ENVIADO_SOCIO]: 'Enviado a Socio',
      [EstadoSolicitud.FIRMADO_SOCIO]: 'Firmado por Socio',
      [EstadoSolicitud.ENVIADO_PETROLERA]: 'Enviado a Petrolera',
      [EstadoSolicitud.ACEPTADA_PETROLERA]: 'Aceptada',
      [EstadoSolicitud.RECHAZADA_PETROLERA]: 'Rechazada'
    };
    return textos[estado] || estado;
  }

  nuevaSolicitud(): void {
    this.router.navigate(['/contratos/nuevo']);
  }

  verDetalle(id: number): void {
    this.router.navigate(['/contratos', id]);
  }

  descargarPdf(solicitud: SolicitudContrato, tipo: 'editable' | 'enviado' | 'firmado' | 'final'): void {
    if (!solicitud.id) return;

    this.solicitudService.descargarPdf(solicitud.id, tipo).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${solicitud.numeroSolicitud}_${tipo}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.notificationService.success('PDF descargado correctamente');
      },
      error: (err) => {
        this.notificationService.error(this.errorHandler.getMensaje(err));
        console.error(err);
      }
    });
  }

  getTipoSolicitudTexto(tipo: TipoSolicitudContrato): string {
    const textos: Record<TipoSolicitudContrato, string> = {
      [TipoSolicitudContrato.NUEVO]: 'Nuevo Contrato',
      [TipoSolicitudContrato.BAJA]: 'Baja de Contrato',
      [TipoSolicitudContrato.CAMBIO_CONDICIONES]: 'Cambio de Condiciones'
    };
    return textos[tipo] || tipo;
  }

  getTipoSolicitudClass(tipo: TipoSolicitudContrato): string {
    const clases: Record<TipoSolicitudContrato, string> = {
      [TipoSolicitudContrato.NUEVO]: 'badge-success',
      [TipoSolicitudContrato.BAJA]: 'badge-danger',
      [TipoSolicitudContrato.CAMBIO_CONDICIONES]: 'badge-warning'
    };
    return clases[tipo] || 'badge-secondary';
  }

  puedeDescargarEditable(solicitud: SolicitudContrato): boolean {
    return !!solicitud.rutaPdfEditable;
  }

  puedeDescargarEnviado(solicitud: SolicitudContrato): boolean {
    return !!solicitud.rutaPdfEnviado;
  }

  puedeDescargarFirmado(solicitud: SolicitudContrato): boolean {
    return !!solicitud.rutaPdfFirmado;
  }

  puedeDescargarFinal(solicitud: SolicitudContrato): boolean {
    return !!solicitud.rutaPdfFinal;
  }
}
