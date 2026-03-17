import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IncidenciaService } from '../../../services/incidencia.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';

@Component({
  selector: 'app-incidencia-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './incidencia-form.html',
  styleUrl: './incidencia-form.css'
})
export class IncidenciaForm {
  form: FormGroup;
  guardando = false;
  error: string | null = null;

  readonly TIPOS = ['BUG', 'MEJORA', 'SUGERENCIA'];
  readonly PRIORIDADES = ['ALTA', 'MEDIA', 'BAJA'];

  constructor(
    private fb: FormBuilder,
    private service: IncidenciaService,
    private router: Router,
    private errorHandler: ErrorHandlerService
  ) {
    this.form = this.fb.group({
      titulo: ['', [Validators.required, Validators.maxLength(200)]],
      descripcion: ['', Validators.required],
      tipo: ['BUG', Validators.required],
      prioridad: ['MEDIA', Validators.required]
    });
  }

  guardar(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.guardando = true;
    this.error = null;
    this.service.create(this.form.value).subscribe({
      next: (inc) => this.router.navigate(['/incidencias', inc.id]),
      error: (err) => { this.guardando = false; this.error = this.errorHandler.getMensaje(err); }
    });
  }

  cancelar(): void {
    this.router.navigate(['/incidencias']);
  }

  get f() { return this.form.controls; }
}
