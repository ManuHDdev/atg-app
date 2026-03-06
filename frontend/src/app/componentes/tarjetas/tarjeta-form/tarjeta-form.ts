import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { TarjetaService } from '../../../services/tarjeta.service';
import { SocioService } from '../../../services/socio.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { Tarjeta } from '../../../models/tarjeta.model';
import { Socio } from '../../../models/socio.model';
import { Petrolera } from '../../../models/petrolera.model';

@Component({
  selector: 'app-tarjeta-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './tarjeta-form.html',
  styleUrl: './tarjeta-form.css'
})
export class TarjetaForm implements OnInit {
  tarjetaForm: FormGroup;
  isEditMode = false;
  tarjetaId: string | null = null;
  loading = false;
  error: string | null = null;

  socios: Socio[] = [];
  petroleras: Petrolera[] = [];
  loadingSocios = false;
  loadingPetroleras = false;

  // Autocomplete Socio
  busquedaSocio: string = '';
  sociosFiltrados: Socio[] = [];
  mostrarListaSocios: boolean = false;
  socioSeleccionado: Socio | null = null;

  constructor(
    private fb: FormBuilder,
    private tarjetaService: TarjetaService,
    private socioService: SocioService,
    private petroleraService: PetroleraService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.tarjetaForm = this.fb.group({
      socioId: ['', Validators.required],
      petroleraId: ['', Validators.required],
      matricula: ['', [Validators.required, Validators.maxLength(20)]],
      numeroContrato: ['', Validators.maxLength(100)],
      activa: [true]
    });
  }

  ngOnInit(): void {
    this.cargarSocios();
    this.cargarPetroleras();

    this.tarjetaId = this.route.snapshot.paramMap.get('id');
    if (this.tarjetaId) {
      this.isEditMode = true;
      this.cargarTarjeta();
    }
  }

  cargarSocios(): void {
    this.loadingSocios = true;
    this.socioService.getAll().subscribe({
      next: (socios) => {
        this.socios = socios.filter(s => s.activo);
        this.loadingSocios = false;
      },
      error: (err) => {
        console.error('Error al cargar socios:', err);
        this.loadingSocios = false;
      }
    });
  }

  cargarPetroleras(): void {
    this.loadingPetroleras = true;
    this.petroleraService.getAll().subscribe({
      next: (petroleras) => {
        this.petroleras = petroleras.filter(p => p.activa);
        this.loadingPetroleras = false;
      },
      error: (err) => {
        console.error('Error al cargar petroleras:', err);
        this.loadingPetroleras = false;
      }
    });
  }

  cargarTarjeta(): void {
    if (!this.tarjetaId) return;

    this.loading = true;
    this.tarjetaService.getById(this.tarjetaId).subscribe({
      next: (tarjeta) => {
        this.tarjetaForm.patchValue({
          socioId: tarjeta.socioId,
          petroleraId: tarjeta.petroleraId,
          matricula: tarjeta.matricula,
          numeroContrato: tarjeta.numeroContrato,
          activa: tarjeta.activa
        });

        // Buscar el socio seleccionado para mostrarlo en el autocomplete
        const socio = this.socios.find(s => String(s.id) === String(tarjeta.socioId));
        if (socio) {
          this.socioSeleccionado = socio;
          this.busquedaSocio = `${socio.nombre} (${socio.numeroSocio})`;
        }

        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar la tarjeta';
        this.loading = false;
        console.error(err);
      }
    });
  }

  // Métodos para autocomplete de socio
  buscarSocios(): void {
    const termino = this.busquedaSocio.toLowerCase().trim();

    if (!termino) {
      this.sociosFiltrados = [];
      this.mostrarListaSocios = false;
      return;
    }

    this.sociosFiltrados = this.socios.filter(socio =>
      socio.nombre.toLowerCase().includes(termino) ||
      socio.numeroSocio.toLowerCase().includes(termino) ||
      (socio.email && socio.email.toLowerCase().includes(termino))
    ).slice(0, 10); // Limitar a 10 resultados

    this.mostrarListaSocios = this.sociosFiltrados.length > 0;
  }

  seleccionarSocio(socio: Socio): void {
    this.socioSeleccionado = socio;
    this.busquedaSocio = `${socio.nombre} (${socio.numeroSocio})`;
    this.tarjetaForm.patchValue({ socioId: socio.id });
    this.mostrarListaSocios = false;
  }

  limpiarBusquedaSocio(): void {
    this.busquedaSocio = '';
    this.socioSeleccionado = null;
    this.sociosFiltrados = [];
    this.mostrarListaSocios = false;
    this.tarjetaForm.patchValue({ socioId: '' });
  }

  onSubmit(): void {
    if (this.tarjetaForm.invalid) {
      this.tarjetaForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const tarjetaData: Tarjeta = this.tarjetaForm.value;

    const operation = this.isEditMode && this.tarjetaId
      ? this.tarjetaService.update(this.tarjetaId, tarjetaData)
      : this.tarjetaService.create(tarjetaData);

    operation.subscribe({
      next: () => {
        this.router.navigate(['/tarjetas']);
      },
      error: (err) => {
        this.error = 'Error al guardar la tarjeta';
        this.loading = false;
        console.error(err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/tarjetas']);
  }

  get f() {
    return this.tarjetaForm.controls;
  }
}
