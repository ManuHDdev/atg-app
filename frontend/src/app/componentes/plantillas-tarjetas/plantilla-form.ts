import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { PlantillaTarjetaService } from '../../services/plantilla-tarjeta.service';
import { PlantillaTarjeta, VARIABLES_DISPONIBLES } from '../../models/plantilla-tarjeta.model';
import { ErrorHandlerService } from '../../services/error-handler.service';

@Component({
  selector: 'app-plantilla-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './plantilla-form.html',
  styleUrl: './plantilla-form.css'
})
export class PlantillaForm implements OnInit {
  formulario!: FormGroup;
  plantillaId: string = '';
  plantilla: PlantillaTarjeta | null = null;
  variablesDisponibles = VARIABLES_DISPONIBLES;

  loading: boolean = false;
  error: string | null = null;
  success: boolean = false;

  constructor(
    private fb: FormBuilder,
    private plantillaService: PlantillaTarjetaService,
    private router: Router,
    private route: ActivatedRoute,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.plantillaId = this.route.snapshot.paramMap.get('id') || '';
    this.inicializarFormulario();
    this.cargarPlantilla();
  }

  inicializarFormulario(): void {
    this.formulario = this.fb.group({
      asunto: ['', [Validators.required, Validators.maxLength(255)]],
      cuerpo: ['', Validators.required],
      activa: [true]
    });
  }

  cargarPlantilla(): void {
    if (!this.plantillaId) return;

    this.loading = true;
    this.plantillaService.getById(this.plantillaId).subscribe({
      next: (plantilla) => {
        this.plantilla = plantilla;
        this.formulario.patchValue({
          asunto: plantilla.asunto,
          cuerpo: plantilla.cuerpo,
          activa: plantilla.activa
        });
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar plantilla:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  insertarVariable(variable: string): void {
    const cuerpoControl = this.formulario.get('cuerpo');
    if (cuerpoControl) {
      const valorActual = cuerpoControl.value || '';
      cuerpoControl.setValue(valorActual + variable);
    }
  }

  onSubmit(): void {
    if (this.formulario.invalid) {
      Object.keys(this.formulario.controls).forEach(key => {
        this.formulario.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;
    this.error = null;

    const plantillaActualizada = {
      ...this.plantilla,
      ...this.formulario.value
    };

    this.plantillaService.update(this.plantillaId, plantillaActualizada).subscribe({
      next: () => {
        this.success = true;
        setTimeout(() => {
          this.router.navigate(['/plantillas-tarjetas']);
        }, 1500);
      },
      error: (err) => {
        console.error('Error al actualizar plantilla:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/plantillas-tarjetas']);
  }

  getTipoLabel(): string {
    if (!this.plantilla) return '';

    const labels: any = {
      'LLEGADA_MADRID': 'Llegada - Madrid',
      'LLEGADA_FUERA': 'Llegada - Otras Provincias',
      'ALTA_SOCIO': 'Alta - Correo al Socio',
      'ALTA_PETROLERA': 'Alta - Correo a Petrolera',
      'ALTA_APROBADA': 'Alta Aprobada - Correo al Socio',
      'ALTA_RECHAZADA': 'Alta Rechazada - Correo al Socio',
      'BAJA_SOCIO': 'Baja - Correo al Socio',
      'DUPLICADO_SOCIO': 'Duplicado - Correo al Socio',
      'DUPLICADO_PETROLERA': 'Duplicado - Correo a Petrolera'
    };
    return labels[this.plantilla.tipo] || this.plantilla.tipo;
  }
}
