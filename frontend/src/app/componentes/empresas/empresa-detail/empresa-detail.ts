import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router, ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { EmpresaService } from '../../../services/empresa.service';
import { SocioService } from '../../../services/socio.service';
import { SolicitudContratoService } from '../../../services/solicitud-contrato.service';
import { TarjetaService } from '../../../services/tarjeta.service';
import { CreditoService } from '../../../services/credito.service';
import { ContratoSocioService } from '../../../services/contrato-socio.service';
import { NotificationService } from '../../../services/notification.service';
import { Empresa } from '../../../models/empresa.model';
import { Socio } from '../../../models/socio.model';
import { SolicitudContrato } from '../../../models/solicitud-contrato.model';
import { Tarjeta } from '../../../models/tarjeta.model';
import { Credito } from '../../../models/credito.model';
import { ContratoSocio } from '../../../models/contrato-socio.model';

type TabType = 'general' | 'solicitudes' | 'activos';

@Component({
  selector: 'app-empresa-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './empresa-detail.html',
  styleUrl: './empresa-detail.css'
})
export class EmpresaDetail implements OnInit {
  empresa: Empresa | null = null;
  socio: Socio | null = null;
  loading = false;
  error: string | null = null;
  empresaId: string | null = null;

  // Tab activo
  activeTab: TabType = 'general';

  // Historial de solicitudes
  solicitudesContrato: SolicitudContrato[] = [];
  solicitudesContratoFiltradas: SolicitudContrato[] = [];
  tarjetasSolicitudes: Tarjeta[] = [];
  solicitudesCredito: Credito[] = [];
  solicitudesCreditoFiltradas: Credito[] = [];
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
  tarjetasActivas: Tarjeta[] = [];
  creditosActivos: Credito[] = [];
  loadingActivos = false;

  constructor(
    private empresaService: EmpresaService,
    private socioService: SocioService,
    private solicitudContratoService: SolicitudContratoService,
    private tarjetaService: TarjetaService,
    private creditoService: CreditoService,
    private contratoSocioService: ContratoSocioService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.empresaId = this.route.snapshot.paramMap.get('id');
    if (this.empresaId) {
      this.cargarEmpresa();
    }
  }

  cargarEmpresa(): void {
    if (!this.empresaId) return;

    this.loading = true;
    this.empresaService.getById(this.empresaId).subscribe({
      next: (empresa) => {
        this.empresa = empresa;
        if (empresa.socioId) {
          this.cargarSocio(empresa.socioId);
        }
        this.loading = false;
      },
      error: (err) => {
        this.notificationService.error('Error al cargar la empresa');
        this.loading = false;
        console.error(err);
      }
    });
  }

  cargarSocio(socioId: string): void {
    this.socioService.getById(socioId).subscribe({
      next: (socio) => {
        this.socio = socio;
      },
      error: (err) => {
        this.notificationService.error('Error al cargar información del socio');
        console.error('Error al cargar socio', err);
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
    if (!this.empresaId) return;

    this.loadingSolicitudes = true;
    const empresaIdNum = Number(this.empresaId);

    // Nota: Necesitas agregar filtro por empresaId en el backend si no existe
    // Por ahora filtraremos del lado del cliente
    forkJoin({
      contratos: this.solicitudContratoService.listar({ size: 1000 }),
      tarjetas: this.tarjetaService.getAll(),
      creditos: this.creditoService.listarTodos()
    }).subscribe({
      next: (result) => {
        // Filtrar solo las solicitudes de esta empresa
        this.solicitudesContrato = result.contratos.content.filter(c => c.empresaId === empresaIdNum);
        this.tarjetasSolicitudes = result.tarjetas; // Ya vienen filtradas si usas tarjetaService.getByEmpresaId cuando lo implementes
        this.solicitudesCredito = result.creditos.filter(c => c.empresaId === empresaIdNum);
        this.aplicarFiltros();
        this.loadingSolicitudes = false;
      },
      error: (err) => {
        console.error('Error al cargar historial de solicitudes', err);
        this.loadingSolicitudes = false;
      }
    });
  }

  cargarObjetosActivos(): void {
    if (!this.empresaId) return;

    this.loadingActivos = true;
    const empresaIdNum = Number(this.empresaId);

    forkJoin({
      contratos: this.contratoSocioService.getAll(),
      tarjetas: this.tarjetaService.getAll(),
      creditos: this.creditoService.listarTodos()
    }).subscribe({
      next: (result) => {
        this.contratosActivos = result.contratos.filter(c => c.activo && c.empresaId === empresaIdNum);
        this.tarjetasActivas = result.tarjetas.filter(t => t.activa);
        this.creditosActivos = result.creditos.filter(c => (c.estado === 'APROBADO' || c.estado === 'COMPLETADO_APROBADO') && c.empresaId === empresaIdNum);
        this.loadingActivos = false;
      },
      error: (err) => {
        console.error('Error al cargar objetos activos', err);
        this.loadingActivos = false;
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

  editarEmpresa(): void {
    if (this.empresaId) {
      this.router.navigate(['/empresas', this.empresaId, 'editar']);
    }
  }

  eliminarEmpresa(): void {
    if (!this.empresaId) return;

    if (confirm('¿Está seguro de eliminar esta empresa?')) {
      this.empresaService.delete(this.empresaId).subscribe({
        next: () => {
          this.notificationService.success('Empresa eliminada correctamente');
          if (this.empresa?.socioId) {
            this.router.navigate(['/empresas'], { queryParams: { socioId: this.empresa.socioId } });
          } else {
            this.router.navigate(['/empresas']);
          }
        },
        error: (err) => {
          this.notificationService.error('Error al eliminar empresa');
          console.error(err);
        }
      });
    }
  }

  volver(): void {
    if (this.empresa?.socioId) {
      this.router.navigate(['/empresas'], { queryParams: { socioId: this.empresa.socioId } });
    } else {
      this.router.navigate(['/empresas']);
    }
  }

  verSocio(): void {
    if (this.empresa?.socioId) {
      this.router.navigate(['/socios', this.empresa.socioId]);
    }
  }

  verDetalleContrato(id: number): void {
    this.router.navigate(['/contratos', id]);
  }

  verDetalleTarjeta(id: string): void {
    this.router.navigate(['/tarjetas', id]);
  }

  verDetalleCredito(id: number): void {
    this.router.navigate(['/creditos', id]);
  }
}
