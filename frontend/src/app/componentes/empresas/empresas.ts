import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router, ActivatedRoute } from '@angular/router';
import { EmpresaService } from '../../services/empresa.service';
import { SocioService } from '../../services/socio.service';
import { NotificationService } from '../../services/notification.service';
import { Empresa } from '../../models/empresa.model';
import { Socio } from '../../models/socio.model';

@Component({
  selector: 'app-empresas',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './empresas.html',
  styleUrl: './empresas.css'
})
export class Empresas implements OnInit {
  empresas: Empresa[] = [];
  loading = false;
  error: string | null = null;
  socioId: string | null = null;
  socioNombre: string = '';
  busquedaEmpresa: string = '';

  get empresasFiltradas(): Empresa[] {
    const termino = this.busquedaEmpresa.toLowerCase().trim();
    if (!termino) return this.empresas;
    return this.empresas.filter(e =>
      e.nombre.toLowerCase().includes(termino) ||
      e.cif.toLowerCase().includes(termino) ||
      (e.email && e.email.toLowerCase().includes(termino)) ||
      (e.socioNombre && e.socioNombre.toLowerCase().includes(termino))
    );
  }

  constructor(
    private empresaService: EmpresaService,
    private socioService: SocioService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    // Verificar si viene desde un socio específico
    this.socioId = this.route.snapshot.queryParamMap.get('socioId');

    if (this.socioId) {
      this.cargarEmpresasDeSocio(this.socioId);
    } else {
      this.cargarTodasEmpresas();
    }
  }

  cargarTodasEmpresas(): void {
    this.loading = true;

    this.empresaService.getAll().subscribe({
      next: (empresas) => {
        this.empresas = empresas || [];
        this.loading = false;
      },
      error: (err) => {
        this.notificationService.error('Error al cargar empresas');
        this.loading = false;
        console.error(err);
      }
    });
  }

  cargarEmpresasDeSocio(socioId: string): void {
    this.loading = true;

    Promise.all([
      this.empresaService.getBySocioId(socioId).toPromise(),
      this.socioService.getById(socioId).toPromise()
    ]).then(([empresas, socio]) => {
      this.empresas = empresas || [];
      this.socioNombre = socio?.nombre || '';
      this.loading = false;
    }).catch((err) => {
      this.notificationService.error('Error al cargar empresas del socio');
      this.loading = false;
      console.error(err);
    });
  }

  verDetalle(id: string): void {
    this.router.navigate(['/empresas', id]);
  }

  nuevaEmpresa(): void {
    if (this.socioId) {
      this.router.navigate(['/empresas/nuevo'], { queryParams: { socioId: this.socioId } });
    } else {
      this.router.navigate(['/empresas/nuevo']);
    }
  }

  editarEmpresa(id: string): void {
    this.router.navigate(['/empresas', id, 'editar']);
  }

  eliminarEmpresa(id: string): void {
    if (confirm('¿Está seguro de eliminar esta empresa?')) {
      this.empresaService.delete(id).subscribe({
        next: () => {
          this.notificationService.success('Empresa eliminada correctamente');
          if (this.socioId) {
            this.cargarEmpresasDeSocio(this.socioId);
          } else {
            this.cargarTodasEmpresas();
          }
        },
        error: (err) => {
          this.notificationService.error('Error al eliminar empresa');
          console.error(err);
        }
      });
    }
  }

  volverASocios(): void {
    if (this.socioId) {
      this.router.navigate(['/socios', this.socioId]);
    } else {
      this.router.navigate(['/socios']);
    }
  }
}
