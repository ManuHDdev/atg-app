import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PlantillaTarjetaService } from '../../services/plantilla-tarjeta.service';
import { PlantillaTarjeta, VARIABLES_DISPONIBLES } from '../../models/plantilla-tarjeta.model';

@Component({
  selector: 'app-plantillas-tarjetas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './plantillas-tarjetas.html',
  styleUrl: './plantillas-tarjetas.css'
})
export class PlantillasTarjetas implements OnInit {
  plantillas: PlantillaTarjeta[] = [];
  loading: boolean = false;
  error: string | null = null;

  constructor(
    private plantillaService: PlantillaTarjetaService,
    private router: Router
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
        this.error = 'Error al cargar las plantillas';
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
      'BAJA_SOCIO': 'Baja - Correo al Socio',
      'DUPLICADO_SOCIO': 'Duplicado - Correo al Socio'
    };
    return labels[tipo] || tipo;
  }

  getTipoIcon(tipo: string): string {
    const icons: any = {
      'LLEGADA_MADRID': '📦',
      'LLEGADA_FUERA': '📮',
      'ALTA_SOCIO': '✉️',
      'ALTA_PETROLERA': '📧',
      'BAJA_SOCIO': '📭',
      'DUPLICADO_SOCIO': '📄'
    };
    return icons[tipo] || '📧';
  }

  volver(): void {
    this.router.navigate(['/solicitudes-tarjetas/dashboard']);
  }
}
