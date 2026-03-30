import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { IncidenciaService } from '../../../services/incidencia.service';
import { AuthService } from '../../../services/auth.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { Incidencia, EstadoIncidencia, ESTADO_LABELS, ESTADOS_INCIDENCIA, TIPO_LABELS } from '../../../models/incidencia.model';

@Component({
  selector: 'app-incidencia-detalle',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './incidencia-detalle.html',
  styleUrl: './incidencia-detalle.css'
})
export class IncidenciaDetalle implements OnInit {
  incidencia: Incidencia | null = null;
  loading = false;
  error: string | null = null;
  comentarioForm: FormGroup;
  estadoForm: FormGroup;
  guardandoComentario = false;
  guardandoEstado = false;

  readonly ESTADO_LABELS = ESTADO_LABELS;
  readonly TIPO_LABELS = TIPO_LABELS;
  readonly ESTADOS = ESTADOS_INCIDENCIA;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private service: IncidenciaService,
    public auth: AuthService,
    private fb: FormBuilder,
    private errorHandler: ErrorHandlerService
  ) {
    this.comentarioForm = this.fb.group({ texto: ['', Validators.required] });
    this.estadoForm = this.fb.group({
      nuevoEstado: ['', Validators.required],
      notasDeveloper: ['']
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.cargar(id);
  }

  cargar(id: number): void {
    this.loading = true;
    this.service.getById(id).subscribe({
      next: (data) => {
        this.incidencia = data;
        this.estadoForm.patchValue({
          nuevoEstado: data.estado,
          notasDeveloper: data.notasDeveloper ?? ''
        });
        this.loading = false;
      },
      error: (err) => { this.error = this.errorHandler.getMensaje(err); this.loading = false; }
    });
  }

  addComentario(): void {
    if (this.comentarioForm.invalid || !this.incidencia) return;
    this.guardandoComentario = true;
    this.service.addComentario(this.incidencia.id, { texto: this.comentarioForm.value.texto }).subscribe({
      next: () => {
        this.comentarioForm.reset();
        this.cargar(this.incidencia!.id);
        this.guardandoComentario = false;
      },
      error: () => { this.guardandoComentario = false; }
    });
  }

  cambiarEstado(): void {
    if (!this.incidencia || this.estadoForm.invalid) return;
    this.guardandoEstado = true;
    this.service.cambiarEstado(this.incidencia.id, this.estadoForm.value).subscribe({
      next: () => { this.cargar(this.incidencia!.id); this.guardandoEstado = false; },
      error: () => { this.guardandoEstado = false; }
    });
  }

  eliminarComentario(comentarioId: number): void {
    if (!this.incidencia || !confirm('¿Eliminar este comentario?')) return;
    this.service.eliminarComentario(this.incidencia.id, comentarioId).subscribe({
      next: () => this.cargar(this.incidencia!.id)
    });
  }

  eliminar(): void {
    if (!this.incidencia || !confirm(`¿Eliminar la incidencia "${this.incidencia.titulo}"? Esta acción no se puede deshacer.`)) return;
    this.service.eliminar(this.incidencia.id).subscribe({
      next: () => this.router.navigate(['/incidencias']),
      error: (err) => { this.error = this.errorHandler.getMensaje(err); }
    });
  }

  volver(): void {
    this.router.navigate(['/incidencias']);
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
}
