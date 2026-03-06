import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { EmpresaService } from '../../../services/empresa.service';
import { SocioService } from '../../../services/socio.service';
import { NotificationService } from '../../../services/notification.service';
import { Empresa } from '../../../models/empresa.model';
import { Socio } from '../../../models/socio.model';

@Component({
  selector: 'app-empresa-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './empresa-form.html',
  styleUrl: './empresa-form.css'
})
export class EmpresaForm implements OnInit {
  empresaForm: FormGroup;
  isEditMode = false;
  empresaId: string | null = null;
  socioIdParam: string | null = null;
  loading = false;
  error: string | null = null;
  fechaAlta: string | null = null;
  socios: Socio[] = [];
  socioSeleccionado: Socio | null = null;
  mostrarSelectorSocio = true;

  constructor(
    private fb: FormBuilder,
    private empresaService: EmpresaService,
    private socioService: SocioService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService
  ) {
    this.empresaForm = this.fb.group({
      socioId: ['', Validators.required],
      nombre: ['', [Validators.required, Validators.maxLength(200)]],
      cif: ['', [Validators.required, Validators.maxLength(20)]],
      direccion: ['', Validators.maxLength(300)],
      poblacion: ['', Validators.maxLength(100)],
      provincia: ['', Validators.maxLength(100)],
      codigoPostal: ['', Validators.maxLength(10)],
      email: ['', [Validators.email, Validators.maxLength(150)]],
      telefono: ['', Validators.maxLength(20)],
      activa: [true]
    });
  }

  ngOnInit(): void {
    this.empresaId = this.route.snapshot.paramMap.get('id');
    this.socioIdParam = this.route.snapshot.queryParamMap.get('socioId');

    if (this.socioIdParam && !this.empresaId) {
      // Si viene de un socio específico para crear nueva empresa, ocultar selector
      this.mostrarSelectorSocio = false;
      this.cargarSocioSeleccionado(this.socioIdParam);
      this.empresaForm.patchValue({ socioId: this.socioIdParam });
    } else {
      // En modo edición o creación sin socio predefinido, cargar lista de socios
      this.cargarSocios();
    }

    if (this.empresaId) {
      this.isEditMode = true;
      this.cargarEmpresa();
    }
  }

  cargarSocioSeleccionado(socioId: string): void {
    this.socioService.getById(socioId).subscribe({
      next: (socio) => {
        this.socioSeleccionado = socio;
      },
      error: (err) => {
        this.notificationService.error('Error al cargar el socio');
        console.error('Error al cargar socio', err);
      }
    });
  }

  cargarSocios(): void {
    this.socioService.getAll().subscribe({
      next: (socios) => {
        this.socios = socios.filter(s => s.activo);
      },
      error: (err) => {
        this.notificationService.error('Error al cargar la lista de socios');
        console.error('Error al cargar socios', err);
      }
    });
  }

  cargarEmpresa(): void {
    if (!this.empresaId) return;

    this.loading = true;
    this.empresaService.getById(this.empresaId).subscribe({
      next: (empresa) => {
        // Establecer todos los valores del formulario
        this.empresaForm.patchValue({
          socioId: String(empresa.socioId), // Convertir a string para el select
          nombre: empresa.nombre,
          cif: empresa.cif,
          direccion: empresa.direccion,
          poblacion: empresa.poblacion,
          provincia: empresa.provincia,
          codigoPostal: empresa.codigoPostal,
          email: empresa.email,
          telefono: empresa.telefono,
          activa: empresa.activa
        });
        // Guardar la fecha de alta para mostrarla (inmutable)
        if (empresa.fechaAlta) {
          this.fechaAlta = new Date(empresa.fechaAlta).toLocaleDateString('es-ES');
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

  onSubmit(): void {
    if (this.empresaForm.invalid) {
      this.empresaForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const empresaData: Empresa = this.empresaForm.value;

    const operation = this.isEditMode && this.empresaId
      ? this.empresaService.update(this.empresaId, empresaData)
      : this.empresaService.create(empresaData);

    operation.subscribe({
      next: () => {
        const mensaje = this.isEditMode ? 'Empresa actualizada correctamente' : 'Empresa creada correctamente';
        this.notificationService.success(mensaje);
        // Volver a la lista de empresas del socio o a todas las empresas
        if (empresaData.socioId) {
          this.router.navigate(['/empresas'], { queryParams: { socioId: empresaData.socioId } });
        } else {
          this.router.navigate(['/empresas']);
        }
      },
      error: (err) => {
        this.notificationService.error('Error al guardar la empresa');
        this.loading = false;
        console.error(err);
      }
    });
  }

  cancelar(): void {
    const socioId = this.empresaForm.value.socioId || this.socioIdParam;
    if (socioId) {
      this.router.navigate(['/empresas'], { queryParams: { socioId } });
    } else {
      this.router.navigate(['/empresas']);
    }
  }

  get f() {
    return this.empresaForm.controls;
  }
}
