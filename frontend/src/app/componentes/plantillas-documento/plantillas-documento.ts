import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PlantillaDocumentoService } from '../../services/plantilla-documento.service';
import { PetroleraService } from '../../services/petrolera.service';
import { NotificationService } from '../../services/notification.service';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { Petrolera } from '../../models/petrolera.model';
import {
  MODULOS_DOCUMENTO,
  ModuloDocumento,
  PlantillaDocumento,
  getModuloLabel,
  getTipoSolicitudLabel
} from '../../models/plantilla-documento.model';

@Component({
  selector: 'app-plantillas-documento',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './plantillas-documento.html',
  styleUrl: './plantillas-documento.css'
})
export class PlantillasDocumento implements OnInit {
  plantillas: PlantillaDocumento[] = [];
  plantillasFiltradas: PlantillaDocumento[] = [];
  petroleras: Petrolera[] = [];
  loading = false;
  error: string | null = null;

  // Filtros
  filtroPetroleraId = '';
  filtroModulo = '';
  filtroActiva = '';

  modulos = MODULOS_DOCUMENTO;

  constructor(
    private plantillaService: PlantillaDocumentoService,
    private petroleraService: PetroleraService,
    private router: Router,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.cargarPetroleras();
    this.cargarPlantillas();
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

  cargarPlantillas(): void {
    this.loading = true;
    this.error = null;

    this.plantillaService.listarTodas().subscribe({
      next: (data) => {
        this.plantillas = data;
        this.aplicarFiltros();
        this.loading = false;
      },
      error: (err) => {
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
        console.error(err);
      }
    });
  }

  aplicarFiltros(): void {
    this.plantillasFiltradas = this.plantillas.filter(plantilla => {
      const cumplePetrolera = !this.filtroPetroleraId
        || String(plantilla.petroleraId) === this.filtroPetroleraId;
      const cumpleModulo = !this.filtroModulo || plantilla.modulo === this.filtroModulo;
      const cumpleActiva = !this.filtroActiva || String(plantilla.activa) === this.filtroActiva;
      return cumplePetrolera && cumpleModulo && cumpleActiva;
    });
  }

  limpiarFiltros(): void {
    this.filtroPetroleraId = '';
    this.filtroModulo = '';
    this.filtroActiva = '';
    this.plantillasFiltradas = this.plantillas;
  }

  getModuloLabel(modulo: ModuloDocumento): string {
    return getModuloLabel(modulo);
  }

  getTipoSolicitudLabel(plantilla: PlantillaDocumento): string {
    return getTipoSolicitudLabel(plantilla.modulo, plantilla.tipoSolicitud);
  }

  nuevaPlantilla(): void {
    this.router.navigate(['/plantillas-documento/nueva']);
  }

  editarPlantilla(id: number): void {
    this.router.navigate(['/plantillas-documento', id, 'editar']);
  }

  cambiarEstado(plantilla: PlantillaDocumento): void {
    if (!plantilla.id) return;

    const nuevoEstado = !plantilla.activa;
    this.plantillaService.cambiarEstado(plantilla.id, nuevoEstado).subscribe({
      next: () => {
        plantilla.activa = nuevoEstado;
        this.aplicarFiltros();
      },
      error: (err) => {
        this.notificationService.error(this.errorHandler.getMensaje(err));
        console.error(err);
      }
    });
  }

  descargarPlantilla(plantilla: PlantillaDocumento): void {
    if (!plantilla.id) return;

    this.plantillaService.descargarArchivo(plantilla.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = plantilla.nombreArchivo || 'plantilla.pdf';
        enlace.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.notificationService.error(this.errorHandler.getMensaje(err));
        console.error(err);
      }
    });
  }

  eliminarPlantilla(id: number): void {
    if (confirm('¿Está seguro de eliminar esta plantilla? Esta acción no se puede deshacer.')) {
      this.plantillaService.eliminar(id).subscribe({
        next: () => {
          this.notificationService.success('Plantilla eliminada correctamente');
          this.cargarPlantillas();
        },
        error: (err) => {
          this.notificationService.error(this.errorHandler.getMensaje(err));
          console.error(err);
        }
      });
    }
  }
}
