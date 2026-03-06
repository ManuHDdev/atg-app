import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PetroleraService } from '../../services/petrolera.service';
import { Petrolera } from '../../models/petrolera.model';

@Component({
  selector: 'app-petroleras',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './petroleras.html',
  styleUrl: './petroleras.css'
})
export class Petroleras implements OnInit {
  petroleras: Petrolera[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private petroleraService: PetroleraService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargarPetroleras();
  }

  cargarPetroleras(): void {
    this.loading = true;
    this.petroleraService.getAll().subscribe({
      next: (data) => {
        this.petroleras = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar petroleras';
        this.loading = false;
        console.error(err);
      }
    });
  }

  verDetalle(id: number, event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    console.log("=== INICIO verDetalle ===");
    console.log("ID recibido:", id);
    console.log("Tipo de ID:", typeof id);
    console.log("ID es undefined?", id === undefined);
    console.log("ID es null?", id === null);

    if (!id) {
      console.error("ERROR: ID es undefined o null");
      return;
    }

    console.log("Navegando a /petroleras/tipos-solicitud con queryParams:", { petroleraId: id });

    this.router.navigate(['/petroleras/tipos-solicitud'], {
      queryParams: { petroleraId: id }
    }).then(success => {
      console.log("Navegación exitosa?", success);
      if (!success) {
        console.error("ERROR: La navegación falló");
      }
    }).catch(error => {
      console.error("ERROR al navegar:", error);
    });

    console.log("=== FIN verDetalle ===");
  }

  nuevaPetrolera(): void {
    this.router.navigate(['/petroleras/nuevo']);
  }

  editarPetrolera(id: number): void {
    this.router.navigate(['/petroleras', id, 'editar']);
  }

  eliminarPetrolera(id: number): void {
    if (confirm('¿Está seguro de eliminar esta petrolera?')) {
      this.petroleraService.delete(id.toString()).subscribe({
        next: () => this.cargarPetroleras(),
        error: (err) => {
          alert('Error al eliminar petrolera');
          console.error(err);
        }
      });
    }
  }
}
