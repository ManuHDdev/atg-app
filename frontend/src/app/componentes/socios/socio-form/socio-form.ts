import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { SocioService } from '../../../services/socio.service';
import { Socio } from '../../../models/socio.model';
import { ErrorHandlerService } from '../../../services/error-handler.service';

@Component({
  selector: 'app-socio-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './socio-form.html',
  styleUrl: './socio-form.css'
})
export class SocioForm implements OnInit {
  socioForm: FormGroup;
  isEditMode = false;
  socioId: string | null = null;
  loading = false;
  error: string | null = null;
  fechaAlta: string | null = null;

  constructor(
    private fb: FormBuilder,
    private socioService: SocioService,
    private router: Router,
    private route: ActivatedRoute,
    private errorHandler: ErrorHandlerService
  ) {
    this.socioForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(200)]],
      direccion: ['', Validators.maxLength(300)],
      poblacion: ['', Validators.maxLength(100)],
      provincia: ['', Validators.maxLength(100)],
      codigoPostal: ['', Validators.maxLength(10)],
      email: ['', [Validators.email, Validators.maxLength(150)]],
      telefono: ['', Validators.maxLength(20)],
      agrupacion: ['ATG', Validators.required],
      numeroSocio: ['', [Validators.required, Validators.maxLength(50)]],
      esAutonomo: [false],
      activo: [true]
    });
  }

  ngOnInit(): void {
    this.socioId = this.route.snapshot.paramMap.get('id');
    if (this.socioId) {
      this.isEditMode = true;
      this.cargarSocio();
    }
  }

  cargarSocio(): void {
    if (!this.socioId) return;

    this.loading = true;
    this.socioService.getById(this.socioId).subscribe({
      next: (socio) => {
        this.socioForm.patchValue(socio);
        // Guardar la fecha de alta para mostrarla (inmutable)
        if (socio.fechaAlta) {
          this.fechaAlta = new Date(socio.fechaAlta).toLocaleDateString('es-ES');
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
        console.error(err);
      }
    });
  }

  onSubmit(): void {
    if (this.socioForm.invalid) {
      this.socioForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const socioData: Socio = this.socioForm.value;

    const operation = this.isEditMode && this.socioId
      ? this.socioService.update(this.socioId, socioData)
      : this.socioService.create(socioData);

    operation.subscribe({
      next: () => {
        this.router.navigate(['/socios']);
      },
      error: (err) => {
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
        console.error(err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/socios']);
  }

  get f() {
    return this.socioForm.controls;
  }
}
