import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { PlantillaTarjetaService } from '../../services/plantilla-tarjeta.service';
import {
  PlantillaTarjeta,
  TipoPlantillaTarjeta,
  TIPOS_PLANTILLA_TARJETA,
  VARIABLES_DISPONIBLES,
  getTipoPlantillaLabel
} from '../../models/plantilla-tarjeta.model';
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

  /** true cuando la ruta no lleva :id (alta de plantilla). */
  modoCreacion: boolean = false;
  /** Tipos que todavía no tienen plantilla creada (solo en modo creación). */
  tiposDisponibles: TipoPlantillaTarjeta[] = [];
  /** true si todos los tipos ya tienen plantilla: no se puede crear ninguna más. */
  sinTiposDisponibles: boolean = false;
  /** Marca que el usuario ya intentó guardar, para mostrar los errores inline. */
  intentoGuardar: boolean = false;

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
    this.modoCreacion = !this.plantillaId;
    this.inicializarFormulario();

    if (this.modoCreacion) {
      this.cargarTiposDisponibles();
    } else {
      this.cargarPlantilla();
    }
  }

  inicializarFormulario(): void {
    this.formulario = this.fb.group({
      asunto: ['', [Validators.required, Validators.maxLength(255)]],
      cuerpo: ['', Validators.required],
      activa: [true]
    });

    if (this.modoCreacion) {
      this.formulario.addControl('tipo', this.fb.control('', Validators.required));
    }
  }

  cargarTiposDisponibles(): void {
    this.loading = true;
    this.plantillaService.getAll().subscribe({
      next: (plantillas) => {
        const tiposUsados = new Set(plantillas.map(p => p.tipo));
        this.tiposDisponibles = TIPOS_PLANTILLA_TARJETA.filter(tipo => !tiposUsados.has(tipo));
        this.sinTiposDisponibles = this.tiposDisponibles.length === 0;
        if (this.sinTiposDisponibles) {
          this.formulario.disable();
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar plantillas:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
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

  campoInvalido(nombre: string): boolean {
    const control = this.formulario.get(nombre);
    if (!control) return false;
    return control.invalid && (this.intentoGuardar || control.touched);
  }

  onSubmit(): void {
    if (this.sinTiposDisponibles) return;

    this.intentoGuardar = true;

    if (this.formulario.invalid) {
      Object.keys(this.formulario.controls).forEach(key => {
        this.formulario.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading = true;
    this.error = null;

    if (this.modoCreacion) {
      this.crearPlantilla();
    } else {
      this.actualizarPlantilla();
    }
  }

  private crearPlantilla(): void {
    const nuevaPlantilla: PlantillaTarjeta = {
      tipo: this.formulario.value.tipo,
      asunto: this.formulario.value.asunto,
      cuerpo: this.formulario.value.cuerpo,
      activa: this.formulario.value.activa
    };

    this.plantillaService.create(nuevaPlantilla).subscribe({
      next: () => {
        this.success = true;
        setTimeout(() => {
          this.router.navigate(['/plantillas-tarjetas']);
        }, 1500);
      },
      error: (err) => {
        console.error('Error al crear plantilla:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  private actualizarPlantilla(): void {
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

  getTipoLabel(tipo?: string): string {
    const valor = tipo ?? this.plantilla?.tipo;
    if (!valor) return '';
    return getTipoPlantillaLabel(valor);
  }

  get tituloPagina(): string {
    return this.modoCreacion ? 'Nueva plantilla' : `Editar Plantilla: ${this.getTipoLabel()}`;
  }

  get textoBotonGuardar(): string {
    return this.modoCreacion ? 'Guardar plantilla' : 'Guardar Cambios';
  }
}
