import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { SocioService } from '../../services/socio.service';
import { Socio } from '../../models/socio.model';

@Component({
  selector: 'app-socios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './socios.html',
  styleUrl: './socios.css'
})
export class Socios implements OnInit {
  socios: Socio[] = [];
  sociosFiltrados: Socio[] = [];
  loading = false;
  error: string | null = null;

  // Buscador
  terminoBusqueda: string = '';

  constructor(
    private socioService: SocioService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarSocios();
  }

  cargarSocios(): void {
    this.loading = true;
    this.socioService.getAll().subscribe({
      next: (data) => {
        this.socios = data;
        this.sociosFiltrados = data;
        this.loading = false;
        console.log(this.socios)
      },
      error: (err) => {
        this.error = 'Error al cargar socios';
        this.loading = false;
        console.error(err);
      }
    });
  }

  buscarSocios(): void {
    const termino = this.terminoBusqueda.toLowerCase().trim();

    if (!termino) {
      this.sociosFiltrados = this.socios;
      return;
    }

    this.sociosFiltrados = this.socios.filter(socio =>
      socio.nombre.toLowerCase().includes(termino) ||
      socio.numeroSocio.toLowerCase().includes(termino) ||
      (socio.email && socio.email.toLowerCase().includes(termino)) ||
      (socio.agrupacion && socio.agrupacion.toLowerCase().includes(termino))
    );
  }

  limpiarBusqueda(): void {
    this.terminoBusqueda = '';
    this.sociosFiltrados = this.socios;
  }

  verDetalle(id: string): void {
    this.router.navigate(['/socios', id]);
  }

  nuevoSocio(): void {
    this.router.navigate(['/socios/nuevo']);
  }

  editarSocio(id: string): void {
    this.router.navigate(['/socios', id, 'editar']);
  }

  eliminarSocio(id: string): void {
    if (confirm('¿Está seguro de eliminar este socio?')) {
      this.socioService.delete(id).subscribe({
        next: () => this.cargarSocios(),
        error: (err) => {
          alert('Error al eliminar socio');
          console.error(err);
        }
      });
    }
  }
}
