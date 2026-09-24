import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PlantillaEmailService } from '../../services/plantilla-email.service';
import { PlantillaEmail, TipoEventoEmail, TIPOS_EVENTO_EMAIL } from '../../models/plantilla-email.model';
import { NotificationService } from '../../services/notification.service';
import { ErrorHandlerService } from '../../services/error-handler.service';

@Component({
  selector: 'app-plantillas-email',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './plantillas-email.html',
  styleUrl: './plantillas-email.css'
})
export class PlantillasEmail implements OnInit {
  plantillas: PlantillaEmail[] = [];
  plantillasFiltradas: PlantillaEmail[] = [];
  loading = false;
  error: string | null = null;

  // Filtros
  filtroTipoEvento: string = '';
  filtroActiva: string = '';

  // Tipos de evento
  TipoEventoEmail = TipoEventoEmail;
  tiposEvento = TIPOS_EVENTO_EMAIL;

  constructor(
    private plantillaEmailService: PlantillaEmailService,
    private router: Router,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.cargarPlantillas();
  }

  cargarPlantillas(): void {
    this.loading = true;
    this.plantillaEmailService.listarTodas().subscribe({
      next: (data) => {
        this.plantillas = data;
        this.plantillasFiltradas = data;
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
      const cumpleTipo = !this.filtroTipoEvento || plantilla.tipoEvento === this.filtroTipoEvento;
      const cumpleActiva = !this.filtroActiva || String(plantilla.activa) === this.filtroActiva;
      return cumpleTipo && cumpleActiva;
    });
  }

  limpiarFiltros(): void {
    this.filtroTipoEvento = '';
    this.filtroActiva = '';
    this.plantillasFiltradas = this.plantillas;
  }

  obtenerLabelTipoEvento(tipo: TipoEventoEmail): string {
    const found = this.tiposEvento.find(t => t.value === tipo);
    return found ? found.label : tipo;
  }

  nuevaPlantilla(): void {
    this.router.navigate(['/plantillas-email/nuevo']);
  }

  editarPlantilla(id: number): void {
    this.router.navigate(['/plantillas-email', id, 'editar']);
  }

  cambiarEstado(plantilla: PlantillaEmail): void {
    if (!plantilla.id) return;

    const nuevoEstado = !plantilla.activa;
    this.plantillaEmailService.cambiarEstado(plantilla.id, nuevoEstado).subscribe({
      next: () => {
        plantilla.activa = nuevoEstado;
      },
      error: (err) => {
        this.notificationService.error(this.errorHandler.getMensaje(err));
        console.error(err);
      }
    });
  }

  eliminarPlantilla(id: number): void {
    if (confirm('¿Está seguro de eliminar esta plantilla? Esta acción no se puede deshacer.')) {
      this.plantillaEmailService.eliminar(id).subscribe({
        next: () => {
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
