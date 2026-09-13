import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PlantillaTarjetaService } from '../../services/plantilla-tarjeta.service';
import { PlantillaTarjeta, VARIABLES_DISPONIBLES } from '../../models/plantilla-tarjeta.model';
import { ErrorHandlerService } from '../../services/error-handler.service';

@Component({
  selector: 'app-plantillas-tarjetas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './plantillas-tarjetas.html',
  styleUrl: './plantillas-tarjetas.css'
})
export class PlantillasTarjetas implements OnInit {
  plantillas: PlantillaTarjeta[] = [];
  variablesDisponibles = VARIABLES_DISPONIBLES;
  loading: boolean = false;
  error: string | null = null;

  constructor(
    private plantillaService: PlantillaTarjetaService,
    private router: Router,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.cargarPlantillas();
  }

  cargarPlantillas(): void {
    this.loading = true;
    this.error = null;

    this.plantillaService.getAll().subscribe({
      next: (data) => {
        this.plantillas = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar plantillas:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  editarPlantilla(id: string): void {
    this.router.navigate(['/plantillas-tarjetas', id, 'editar']);
  }

  getTipoLabel(tipo: string): string {
    const labels: any = {
      'LLEGADA_MADRID': 'Llegada - Madrid',
      'LLEGADA_FUERA': 'Llegada - Otras Provincias',
      'ALTA_SOCIO': 'Alta - Correo al Socio',
      'ALTA_PETROLERA': 'Alta - Correo a Petrolera',
      'ALTA_APROBADA': 'Alta - Aprobada por la Petrolera',
      'ALTA_RECHAZADA': 'Alta - Rechazada por la Petrolera',
      'BAJA_SOCIO': 'Baja - Correo al Socio',
      'BAJA_CONFIRMADA': 'Baja - Confirmada por la Petrolera',
      'DUPLICADO_SOCIO': 'Duplicado - Correo al Socio',
      'DUPLICADO_CONFIRMADA': 'Duplicado - Confirmado por la Petrolera',
      'DUPLICADO_PETROLERA': 'Duplicado - Correo a Petrolera'
    };
    return labels[tipo] || tipo;
  }

  getTipoIcon(tipo: string): string {
    const icons: any = {
      'LLEGADA_MADRID': 'bi-box-seam',
      'LLEGADA_FUERA': 'bi-mailbox',
      'ALTA_SOCIO': 'bi-envelope',
      'ALTA_PETROLERA': 'bi-envelope-fill',
      'ALTA_APROBADA': 'bi-check-circle',
      'ALTA_RECHAZADA': 'bi-x-circle',
      'BAJA_SOCIO': 'bi-envelope-open',
      'BAJA_CONFIRMADA': 'bi-check2-square',
      'DUPLICADO_SOCIO': 'bi-file-earmark',
      'DUPLICADO_CONFIRMADA': 'bi-file-earmark-check',
      'DUPLICADO_PETROLERA': 'bi-file-earmark-arrow-up'
    };
    return icons[tipo] || 'bi-envelope-fill';
  }

  volver(): void {
    this.router.navigate(['/solicitudes-tarjetas/dashboard']);
  }
}
