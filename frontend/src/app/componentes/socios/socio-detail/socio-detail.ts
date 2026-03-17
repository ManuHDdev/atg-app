import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router, ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { SocioService } from '../../../services/socio.service';
import { EmpresaService } from '../../../services/empresa.service';
import { SolicitudContratoService } from '../../../services/solicitud-contrato.service';
import { TarjetaService } from '../../../services/tarjeta.service';
import { SolicitudTarjetaService } from '../../../services/solicitud-tarjeta.service';
import { CreditoService } from '../../../services/credito.service';
import { ContratoSocioService } from '../../../services/contrato-socio.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { DispositivoService } from '../../../services/dispositivo.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { Socio } from '../../../models/socio.model';
import { Empresa } from '../../../models/empresa.model';
import { SolicitudContrato } from '../../../models/solicitud-contrato.model';
import { Tarjeta } from '../../../models/tarjeta.model';
import { SolicitudTarjeta } from '../../../models/solicitud-tarjeta.model';
import { Credito } from '../../../models/credito.model';
import { ContratoSocio } from '../../../models/contrato-socio.model';
import { Petrolera } from '../../../models/petrolera.model';
import { SolicitudDispositivo } from '../../../models/dispositivo.model';

type TabType = 'general' | 'solicitudes' | 'activos';

@Component({
  selector: 'app-socio-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './socio-detail.html',
  styleUrl: './socio-detail.css',
})
export class SocioDetail implements OnInit {
  socio: Socio | null = null;
  empresas: Empresa[] = [];
  loading = false;
  error: string | null = null;
  socioId: string | null = null;

  // Tab activo
  activeTab: TabType = 'general';

  // Historial de solicitudes
  solicitudesContrato: SolicitudContrato[] = [];
  solicitudesContratoFiltradas: SolicitudContrato[] = [];
  tarjetasSolicitudes: SolicitudTarjeta[] = [];
  tarjetas: Tarjeta[] = [];
  solicitudesCredito: Credito[] = [];
  solicitudesCreditoFiltradas: Credito[] = [];
  solicitudesDispositivo: SolicitudDispositivo[] = [];
  loadingSolicitudes = false;

  // Filtros de historial
  filtroEstadoContrato: string = '';
  filtroTipoSolicitud: string = '';
  filtroEstadoCredito: string = '';
  filtroFechaDesde: string = '';
  filtroFechaHasta: string = '';

  // Paginación historial
  paginaContratosActual = 0;
  tamañoPaginaContratos = 10;
  totalPaginasContratos = 0;

  paginaCreditosActual = 0;
  tamañoPaginaCreditos = 10;
  totalPaginasCreditos = 0;

  // Objetos activos
  contratosActivos: ContratoSocio[] = [];
  contratosInactivos: ContratoSocio[] = [];
  tarjetasActivas: Tarjeta[] = [];
  tarjetasInactivas: Tarjeta[] = [];
  creditosActivos: Credito[] = [];
  loadingActivos = false;
  petroleras: Petrolera[] = [];

  constructor(
    private socioService: SocioService,
    private empresaService: EmpresaService,
    private solicitudContratoService: SolicitudContratoService,
    private tarjetaService: TarjetaService,
    private solicitudTarjetaService: SolicitudTarjetaService,
    private creditoService: CreditoService,
    private contratoSocioService: ContratoSocioService,
    private petroleraService: PetroleraService,
    private dispositivoService: DispositivoService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.socioId = this.route.snapshot.paramMap.get('id');
    if (this.socioId) {
      this.cargarSocio();
      this.cargarEmpresas();
    }
  }

  cargarSocio(): void {
    if (!this.socioId) return;

    this.loading = true;
    this.socioService.getById(this.socioId).subscribe({
      next: (socio) => {
        this.socio = socio;
        this.loading = false;
      },
      error: (err) => {
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
        console.error(err);
      }
    });
  }

  cargarEmpresas(): void {
    if (!this.socioId) return;

    this.empresaService.getBySocioId(this.socioId).subscribe({
      next: (empresas) => {
        this.empresas = empresas;
      },
      error: (err) => {
        console.error('Error al cargar empresas', err);
      }
    });
  }

  cambiarTab(tab: TabType): void {
    this.activeTab = tab;

    if (tab === 'solicitudes' && this.solicitudesContrato.length === 0) {
      this.cargarHistorialSolicitudes();
    } else if (tab === 'activos' && this.contratosActivos.length === 0) {
      this.cargarObjetosActivos();
    }
  }

  cargarHistorialSolicitudes(): void {
    if (!this.socioId) return;

    this.loadingSolicitudes = true;
    const socioIdNum = Number(this.socioId);

    forkJoin({
      contratos: this.solicitudContratoService.listar({ socioId: socioIdNum, size: 1000 }),
      solicitudesTarjeta: this.solicitudTarjetaService.getBySocioId(this.socioId),
      creditos: this.creditoService.listarPorSocio(socioIdNum),
      dispositivos: this.dispositivoService.listarSolicitudesPorSocio(socioIdNum)
    }).subscribe({
      next: (result) => {
        this.solicitudesContrato = result.contratos.content;
        this.tarjetasSolicitudes = result.solicitudesTarjeta;
        this.solicitudesCredito = result.creditos;
        this.solicitudesDispositivo = result.dispositivos;
        this.aplicarFiltros();
        this.loadingSolicitudes = false;
      },
      error: (err) => {
        console.error('Error al cargar historial de solicitudes', err);
        this.loadingSolicitudes = false;
      }
    });
  }

  aplicarFiltros(): void {
    // Filtrar contratos
    let contratosFiltrados = [...this.solicitudesContrato];

    if (this.filtroEstadoContrato) {
      contratosFiltrados = contratosFiltrados.filter(c => c.estado === this.filtroEstadoContrato);
    }

    if (this.filtroTipoSolicitud) {
      contratosFiltrados = contratosFiltrados.filter(c => c.tipoSolicitud === this.filtroTipoSolicitud);
    }

    if (this.filtroFechaDesde) {
      const fechaDesde = new Date(this.filtroFechaDesde);
      contratosFiltrados = contratosFiltrados.filter(c =>
        c.fechaHoraSolicitud && new Date(c.fechaHoraSolicitud) >= fechaDesde
      );
    }

    if (this.filtroFechaHasta) {
      const fechaHasta = new Date(this.filtroFechaHasta);
      fechaHasta.setHours(23, 59, 59, 999);
      contratosFiltrados = contratosFiltrados.filter(c =>
        c.fechaHoraSolicitud && new Date(c.fechaHoraSolicitud) <= fechaHasta
      );
    }

    this.solicitudesContratoFiltradas = contratosFiltrados;
    this.totalPaginasContratos = Math.ceil(contratosFiltrados.length / this.tamañoPaginaContratos);
    this.paginaContratosActual = 0;

    // Filtrar créditos
    let creditosFiltrados = [...this.solicitudesCredito];

    if (this.filtroEstadoCredito) {
      creditosFiltrados = creditosFiltrados.filter(c => c.estado === this.filtroEstadoCredito);
    }

    if (this.filtroFechaDesde) {
      const fechaDesde = new Date(this.filtroFechaDesde);
      creditosFiltrados = creditosFiltrados.filter(c =>
        c.createdAt && new Date(c.createdAt) >= fechaDesde
      );
    }

    if (this.filtroFechaHasta) {
      const fechaHasta = new Date(this.filtroFechaHasta);
      fechaHasta.setHours(23, 59, 59, 999);
      creditosFiltrados = creditosFiltrados.filter(c =>
        c.createdAt && new Date(c.createdAt) <= fechaHasta
      );
    }

    this.solicitudesCreditoFiltradas = creditosFiltrados;
    this.totalPaginasCreditos = Math.ceil(creditosFiltrados.length / this.tamañoPaginaCreditos);
    this.paginaCreditosActual = 0;
  }

  limpiarFiltros(): void {
    this.filtroEstadoContrato = '';
    this.filtroTipoSolicitud = '';
    this.filtroEstadoCredito = '';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.aplicarFiltros();
  }

  get contratosContratosPaginados(): SolicitudContrato[] {
    const inicio = this.paginaContratosActual * this.tamañoPaginaContratos;
    const fin = inicio + this.tamañoPaginaContratos;
    return this.solicitudesContratoFiltradas.slice(inicio, fin);
  }

  get solicitudesCreditoPaginadas(): Credito[] {
    const inicio = this.paginaCreditosActual * this.tamañoPaginaCreditos;
    const fin = inicio + this.tamañoPaginaCreditos;
    return this.solicitudesCreditoFiltradas.slice(inicio, fin);
  }

  cambiarPaginaContratos(pagina: number): void {
    this.paginaContratosActual = pagina;
  }

  cambiarPaginaCreditos(pagina: number): void {
    this.paginaCreditosActual = pagina;
  }

  cargarObjetosActivos(): void {
    if (!this.socioId) return;

    this.loadingActivos = true;
    const socioIdNum = Number(this.socioId);

    forkJoin({
      contratos: this.contratoSocioService.getBySocioId(socioIdNum),
      tarjetas: this.tarjetaService.getBySocioId(this.socioId),
      creditos: this.creditoService.listarPorSocio(socioIdNum),
      petroleras: this.petroleraService.listar()
    }).subscribe({
      next: (result) => {
        this.petroleras = result.petroleras;
        this.contratosActivos = result.contratos.filter(c => c.activo);
        this.contratosInactivos = result.contratos.filter(c => !c.activo);
        this.tarjetasActivas = result.tarjetas.filter(t => t.activa);
        this.tarjetasInactivas = result.tarjetas.filter(t => !t.activa);
        this.creditosActivos = result.creditos.filter(c => c.estado === 'APROBADO' || c.estado === 'COMPLETADO_APROBADO');
        this.loadingActivos = false;
      },
      error: (err) => {
        console.error('Error al cargar objetos activos', err);
        this.loadingActivos = false;
      }
    });
  }

  obtenerNombreEmpresa(empresaId?: number | string): string {
    if (!empresaId) return '';
    const empresa = this.empresas.find(e => e.id === empresaId.toString());
    return empresa ? empresa.nombre : 'Empresa desconocida';
  }

  esDeEmpresa(empresaId?: number | string): boolean {
    return !!empresaId;
  }

  editarSocio(): void {
    if (this.socioId) {
      this.router.navigate(['/socios', this.socioId, 'editar']);
    }
  }

  eliminarSocio(): void {
    if (!this.socioId) return;

    if (confirm('¿Está seguro de eliminar este socio?')) {
      this.socioService.delete(this.socioId).subscribe({
        next: () => {
          this.router.navigate(['/socios']);
        },
        error: (err) => {
          this.notificationService.error(this.errorHandler.getMensaje(err));
          console.error(err);
        }
      });
    }
  }

  volver(): void {
    this.router.navigate(['/socios']);
  }

  verEmpresa(empresaId: string): void {
    this.router.navigate(['/empresas', empresaId]);
  }

  nuevaEmpresa(): void {
    if (this.socioId) {
      this.router.navigate(['/empresas/nuevo'], { queryParams: { socioId: this.socioId } });
    }
  }

  gestionarEmpresas(): void {
    if (this.socioId) {
      this.router.navigate(['/empresas'], { queryParams: { socioId: this.socioId } });
    }
  }

  verDetalleContrato(id: number): void {
    this.router.navigate(['/contratos', id]);
  }

  verDetalleSolicitudContrato(contrato: ContratoSocio): void {
    if (contrato.solicitudId) {
      this.router.navigate(['/contratos', contrato.solicitudId]);
    }
  }

  verDetalleTarjeta(id: string): void {
    this.router.navigate(['/tarjetas', id]);
  }

  verDetalleSolicitudTarjeta(id: string): void {
    this.router.navigate(['/solicitudes-tarjetas/detalle', id]);
  }

  verDetalleCredito(id: number): void {
    this.router.navigate(['/creditos', id]);
  }

  verDetalleSolicitudDispositivo(id: number): void {
    this.router.navigate(['/dispositivos/solicitudes', id]);
  }

  obtenerNombrePetrolera(petroleraId?: number): string {
    if (!petroleraId) return '';
    const petrolera = this.petroleras.find(p => p.id === petroleraId);
    return petrolera ? petrolera.nombre : 'Petrolera desconocida';
  }

  obtenerPetroleraConSubtipo(contrato: ContratoSocio): string {
    const petrolera = this.obtenerNombrePetrolera(contrato.petroleraId);
    if (contrato.subtipoContrato) {
      return `${petrolera} - ${contrato.subtipoContrato}`;
    }
    return petrolera;
  }

  obtenerTipoContratoTexto(contrato: ContratoSocio): string {
    const tipos: Record<string, string> = {
      'NUEVO': 'Nuevo Contrato',
      'BAJA': 'Baja',
      'CAMBIO_CONDICIONES': 'Cambio de Condiciones'
    };
    return tipos[contrato.tipoContrato || ''] || contrato.tipoContrato || '-';
  }
}
