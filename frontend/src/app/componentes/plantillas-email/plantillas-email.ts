import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PlantillaEmailService } from '../../services/plantilla-email.service';
import { PlantillaEmail, TipoEventoEmail } from '../../models/plantilla-email.model';

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
  tiposEvento = [
    { value: TipoEventoEmail.SOLICITUD_CREDITO, label: 'Crédito: Solicitud' },
    { value: TipoEventoEmail.AMPLIACION_CREDITO, label: 'Crédito: Ampliación' },
    { value: TipoEventoEmail.DEVOLUCION_AVAL, label: 'Crédito: Devolución de Aval' },
    { value: TipoEventoEmail.NOTIF_SOCIO_CREADO, label: 'Crédito: Notif. Trámite Registrado' },
    { value: TipoEventoEmail.NOTIF_SOCIO_ENVIADO, label: 'Crédito: Notif. Enviado a Petrolera' },
    { value: TipoEventoEmail.NOTIF_SOCIO_RESULTADO, label: 'Crédito: Notif. Resultado' },
    { value: TipoEventoEmail.CONTRATO_PETROLERA, label: 'Contrato: Email a Petrolera' },
    { value: TipoEventoEmail.NOTIF_SOCIO_CONTRATO_CREADO, label: 'Contrato: Notif. Socio - Solicitud Registrada' },
    { value: TipoEventoEmail.NOTIF_SOCIO_CONTRATO_ENVIADO, label: 'Contrato: Notif. Socio - Contrato Enviado' },
    { value: TipoEventoEmail.NOTIF_SOCIO_CONTRATO_RESULTADO, label: 'Contrato: Notif. Socio - Resultado' },
    { value: TipoEventoEmail.ALTA_DISPOSITIVO, label: 'Dispositivo: Alta' },
    { value: TipoEventoEmail.SOLICITUD_CREDITO_DISPOSITIVO, label: 'Dispositivo: Solicitud de Crédito' },
    { value: TipoEventoEmail.BAJA_DISPOSITIVO, label: 'Dispositivo: Baja' },
    { value: TipoEventoEmail.CAMBIO_MATRICULA, label: 'Dispositivo: Cambio de Matrícula' },
    { value: TipoEventoEmail.NOTIF_SOCIO_DISP_CREADO, label: 'Dispositivo: Notif. Solicitud Registrada' },
    { value: TipoEventoEmail.NOTIF_SOCIO_DISP_ENVIADO, label: 'Dispositivo: Notif. Enviado a Petrolera' },
    { value: TipoEventoEmail.NOTIF_SOCIO_DISP_RESULTADO, label: 'Dispositivo: Notif. Resultado' }
  ];

  constructor(
    private plantillaEmailService: PlantillaEmailService,
    private router: Router
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
        this.error = 'Error al cargar plantillas';
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
        alert('Error al cambiar el estado de la plantilla');
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
          alert('Error al eliminar plantilla');
          console.error(err);
        }
      });
    }
  }
}
