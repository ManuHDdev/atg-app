import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { IncidenciaService } from '../../services/incidencia.service';
import { AuthService } from '../../services/auth.service';
import { IncidenciaResumen, EstadoIncidencia, ESTADOS_INCIDENCIA, ESTADO_LABELS, TIPO_LABELS } from '../../models/incidencia.model';

@Component({
  selector: 'app-incidencias',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './incidencias.html',
  styleUrl: './incidencias.css'
})
export class Incidencias implements OnInit {
  incidencias: IncidenciaResumen[] = [];
  loading = false;
  error: string | null = null;
  filtroEstado: EstadoIncidencia | '' = '';

  readonly ESTADO_LABELS = ESTADO_LABELS;
  readonly TIPO_LABELS = TIPO_LABELS;
  readonly ESTADOS_FILTRO = ESTADOS_INCIDENCIA;

  constructor(
    private service: IncidenciaService,
    public auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading = true;
    this.error = null;
    this.service.getAll().subscribe({
      next: (data) => { this.incidencias = data; this.loading = false; },
      error: () => { this.error = 'Error al cargar las incidencias'; this.loading = false; }
    });
  }

  get incidenciasFiltradas(): IncidenciaResumen[] {
    if (!this.filtroEstado) return this.incidencias;
    return this.incidencias.filter(i => i.estado === this.filtroEstado);
  }

  verDetalle(id: number): void {
    this.router.navigate(['/incidencias', id]);
  }

  nueva(): void {
    this.router.navigate(['/incidencias/nueva']);
  }

  estadoClass(estado: EstadoIncidencia): string {
    const map: Record<EstadoIncidencia, string> = {
      NUEVA: 'estado-nueva',
      EN_REVISION: 'estado-revision',
      EN_DESARROLLO: 'estado-desarrollo',
      PENDIENTE_DEPLOY: 'estado-pendiente',
      EN_PRODUCCION: 'estado-produccion',
      DESCARTADA: 'estado-descartada'
    };
    return map[estado] ?? '';
  }

  prioridadClass(p: string): string {
    if (p === 'ALTA') return 'prioridad-alta';
    if (p === 'MEDIA') return 'prioridad-media';
    return 'prioridad-baja';
  }

  tipoClass(t: string): string {
    if (t === 'BUG') return 'tipo-bug';
    if (t === 'MEJORA') return 'tipo-mejora';
    return 'tipo-sugerencia';
  }
}
