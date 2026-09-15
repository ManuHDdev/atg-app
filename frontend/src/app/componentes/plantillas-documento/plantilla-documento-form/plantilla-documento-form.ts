import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PlantillaDocumentoService } from '../../../services/plantilla-documento.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { Petrolera } from '../../../models/petrolera.model';
import {
  MODULOS_DOCUMENTO,
  ModuloDocumento,
  PlantillaDocumento,
  TIPOS_SOLICITUD_POR_MODULO
} from '../../../models/plantilla-documento.model';

@Component({
  selector: 'app-plantilla-documento-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './plantilla-documento-form.html',
  styleUrls: ['./plantilla-documento-form.css']
})
export class PlantillaDocumentoForm implements OnInit {
  petroleraId = 0;
  modulo: ModuloDocumento = ModuloDocumento.TARJETAS;
  tipoSolicitud = '';
  archivo: File | null = null;
  nombreArchivoActual: string | null = null;

  petroleras: Petrolera[] = [];
  modulos = MODULOS_DOCUMENTO;

  plantillaId?: number;
  isEditMode = false;
  loading = false;
  guardando = false;
  error = '';

  constructor(
    private plantillaService: PlantillaDocumentoService,
    private petroleraService: PetroleraService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.cargarPetroleras();

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.plantillaId = Number(id);
      this.isEditMode = true;
      this.cargarPlantilla();
    }
  }

  get tiposSolicitud(): { value: string; label: string }[] {
    return TIPOS_SOLICITUD_POR_MODULO[this.modulo] ?? [];
  }

  cargarPetroleras(): void {
    this.petroleraService.listar().subscribe({
      next: (data) => {
        this.petroleras = data.filter(p => p.activa);
      },
      error: (err) => {
        console.error('Error al cargar petroleras:', err);
      }
    });
  }

  cargarPlantilla(): void {
    if (!this.plantillaId) return;

    this.loading = true;
    this.plantillaService.obtenerPorId(this.plantillaId).subscribe({
      next: (plantilla: PlantillaDocumento) => {
        this.petroleraId = plantilla.petroleraId;
        this.modulo = plantilla.modulo;
        this.tipoSolicitud = plantilla.tipoSolicitud;
        this.nombreArchivoActual = plantilla.nombreArchivo ?? null;
        this.loading = false;
      },
      error: (err) => {
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
        console.error(err);
      }
    });
  }

  /** Al cambiar de módulo, el tipo de solicitud anterior deja de ser válido. */
  onModuloChange(): void {
    this.tipoSolicitud = '';
  }

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const seleccionado = input.files?.[0] ?? null;

    if (seleccionado && seleccionado.type !== 'application/pdf') {
      this.error = 'El archivo debe ser un PDF';
      this.archivo = null;
      input.value = '';
      return;
    }

    this.error = '';
    this.archivo = seleccionado;
  }

  get formularioValido(): boolean {
    if (this.isEditMode) {
      return this.archivo !== null;
    }
    return this.petroleraId > 0 && !!this.tipoSolicitud && this.archivo !== null;
  }

  guardar(): void {
    if (!this.formularioValido || this.guardando) return;

    this.guardando = true;
    this.error = '';

    const peticion = this.isEditMode
      ? this.plantillaService.reemplazarArchivo(this.plantillaId!, this.archivo!)
      : this.plantillaService.crear(Number(this.petroleraId), this.modulo, this.tipoSolicitud, this.archivo!);

    peticion.subscribe({
      next: () => {
        this.guardando = false;
        this.notificationService.success(
          this.isEditMode ? 'Plantilla actualizada correctamente' : 'Plantilla creada correctamente'
        );
        this.router.navigate(['/plantillas-documento']);
      },
      error: (err) => {
        this.guardando = false;
        this.error = this.errorHandler.getMensaje(err);
        console.error(err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/plantillas-documento']);
  }
}
