import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TarjetaService } from '../../services/tarjeta.service';
import { SocioService } from '../../services/socio.service';
import { PetroleraService } from '../../services/petrolera.service';
import { Tarjeta } from '../../models/tarjeta.model';
import { Socio } from '../../models/socio.model';
import { Petrolera } from '../../models/petrolera.model';

@Component({
  selector: 'app-tarjetas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tarjetas.html',
  styleUrl: './tarjetas.css'
})
export class Tarjetas implements OnInit {
  tarjetas: Tarjeta[] = [];
  tarjetasFiltradas: Tarjeta[] = [];
  socios: Socio[] = [];
  petroleras: Petrolera[] = [];
  loading = false;
  error: string | null = null;
  mostrarModalEliminar = false;
  tarjetaIdAEliminar: string | null = null;

  // Filtros
  filtroSocioId: string = '';
  filtroPetroleraId: string = '';

  // Autocomplete Socio
  busquedaSocio: string = '';
  sociosFiltrados: Socio[] = [];
  mostrarListaSocios: boolean = false;
  socioSeleccionado: Socio | null = null;

  constructor(
    private tarjetaService: TarjetaService,
    private socioService: SocioService,
    private petroleraService: PetroleraService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading = true;
    
    // Cargar tarjetas, socios y petroleras en paralelo
    Promise.all([
      this.tarjetaService.getAll().toPromise(),
      this.socioService.getAll().toPromise(),
      this.petroleraService.getAll().toPromise()
    ]).then(([tarjetas, socios, petroleras]) => {
      this.tarjetas = tarjetas || [];
      this.tarjetasFiltradas = this.tarjetas;
      this.socios = socios || [];
      this.petroleras = petroleras || [];
      this.loading = false;
    }).catch((err) => {
      this.error = 'Error al cargar datos';
      this.loading = false;
      console.error(err);
    });
  }

  aplicarFiltros(): void {
    this.tarjetasFiltradas = this.tarjetas.filter(tarjeta => {
      const cumpleFiltroSocio = !this.filtroSocioId || String(tarjeta.socioId) === String(this.filtroSocioId);
      const cumpleFiltroPetrolera = !this.filtroPetroleraId || String(tarjeta.petroleraId) === String(this.filtroPetroleraId);
      return cumpleFiltroSocio && cumpleFiltroPetrolera;
    });
  }

  limpiarFiltros(): void {
    this.filtroSocioId = '';
    this.filtroPetroleraId = '';
    this.busquedaSocio = '';
    this.socioSeleccionado = null;
    this.tarjetasFiltradas = this.tarjetas;
  }

  // Métodos para autocomplete de socio
  buscarSocios(): void {
    const termino = this.busquedaSocio.toLowerCase().trim();

    if (!termino) {
      this.sociosFiltrados = [];
      this.mostrarListaSocios = false;
      this.filtroSocioId = '';
      this.socioSeleccionado = null;
      this.aplicarFiltros();
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
    this.filtroSocioId = String(socio.id);
    this.mostrarListaSocios = false;
    this.aplicarFiltros();
  }

  limpiarBusquedaSocio(): void {
    this.busquedaSocio = '';
    this.socioSeleccionado = null;
    this.filtroSocioId = '';
    this.sociosFiltrados = [];
    this.mostrarListaSocios = false;
    this.aplicarFiltros();
  }

  obtenerNombreSocio(socioId: string): string {
    const socio = this.socios.find(s => String(s.id) === String(socioId));
    return socio ? socio.nombre : 'Desconocido';
  }

  obtenerNombrePetrolera(petroleraId: string): string {
    const petrolera = this.petroleras.find(p => String(p.id) === String(petroleraId));
    return petrolera ? petrolera.nombre : 'Desconocida';
  }

  nuevaTarjeta(): void {
    this.router.navigate(['/tarjetas/nuevo']);
  }

  editarTarjeta(id: string): void {
    this.router.navigate(['/tarjetas', id, 'editar']);
  }

  abrirModalEliminar(id: string): void {
    this.tarjetaIdAEliminar = id;
    this.mostrarModalEliminar = true;
  }

  cancelarEliminar(): void {
    this.mostrarModalEliminar = false;
    this.tarjetaIdAEliminar = null;
  }

  confirmarEliminar(): void {
    if (!this.tarjetaIdAEliminar) return;
    this.loading = true;
    this.tarjetaService.delete(this.tarjetaIdAEliminar).subscribe({
      next: () => {
        this.mostrarModalEliminar = false;
        this.tarjetaIdAEliminar = null;
        this.loading = false;
        this.cargarDatos();
      },
      error: (err) => {
        this.error = 'Error al eliminar la tarjeta';
        this.loading = false;
        console.error(err);
      }
    });
  }
}
