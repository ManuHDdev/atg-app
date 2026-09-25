import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TipoSolicitudService } from '../../../services/tipo-solicitud.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { TipoSolicitud } from '../../../models/tipo-solicitud.model';
import { Petrolera } from '../../../models/petrolera.model';
import { ZonaSoltarArchivo } from '../../shared/zona-soltar-archivo/zona-soltar-archivo';

@Component({
  selector: 'app-tipos-solicitud',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ZonaSoltarArchivo],
  templateUrl: './tipos-solicitud.html',
  styleUrl: './tipos-solicitud.css'
})
export class TiposSolicitud implements OnInit {
  tiposSolicitud: TipoSolicitud[] = [];
  petroleras: Petrolera[] = [];
  petroleraId: string | null = null;
  petroleraNombre = '';
  /**
   * La pantalla tiene dos entradas: el enlace "Ver" de cada fila de petroleras, que llega con
   * ?petroleraId=, y la entrada del menú lateral, que llega sin parámetro. En el primer caso la
   * petrolera viene fijada por la ruta y no se ofrece el selector; en el segundo, el usuario la
   * elige aquí mismo y esa elección alimenta exactamente el mismo listado.
   */
  petroleraFijadaPorRuta = false;
  loading = false;

  // Formulario para añadir/editar tipo de solicitud
  tipoSolicitudForm: FormGroup;
  editandoId: string | null = null;
  editandoOrden: number | null = null;
  mostrarFormulario = false;

  // Manejo de archivo PDF
  archivoSeleccionado: File | null = null;
  nombreArchivoMostrar = '';

  // Modal previsualización PDF
  mostrarModalPdf = false;
  urlPdfModal: SafeResourceUrl | null = null;
  private _blobUrlPdf: string | null = null;

  constructor(
    private fb: FormBuilder,
    private tipoSolicitudService: TipoSolicitudService,
    private petroleraService: PetroleraService,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService,
    private sanitizer: DomSanitizer,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.tipoSolicitudForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      activa: [true]
    });
  }

  ngOnInit(): void {
    this.petroleraId = this.route.snapshot.queryParamMap.get('petroleraId');
    this.petroleraFijadaPorRuta = !!this.petroleraId;

    if (this.petroleraId) {
      this.cargarTiposSolicitudDePetrolera(this.petroleraId);
    }

    this.cargarPetroleras();
  }

  /** Cambio en el selector de petrolera: sustituye el ámbito del listado sin salir de la pantalla. */
  onPetroleraSeleccionada(petroleraId: string): void {
    this.cancelarFormulario();
    this.petroleraId = petroleraId || null;
    this.tiposSolicitud = [];
    // Se pinta ya el nombre elegido para que la cabecera no parpadee mientras llega la respuesta.
    this.petroleraNombre = this.petroleras.find(p => String(p.id) === petroleraId)?.nombre || '';

    if (this.petroleraId) {
      this.cargarTiposSolicitudDePetrolera(this.petroleraId);
    }
  }

  cargarTiposSolicitudDePetrolera(petroleraId: string): void {
    this.loading = true;
    Promise.all([
      this.tipoSolicitudService.getByPetroleraId(petroleraId).toPromise(),
      this.petroleraService.getById(petroleraId).toPromise()
    ]).then(([tipos, petrolera]) => {
      this.tiposSolicitud = (tipos || []).sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
      this.petroleraNombre = petrolera?.nombre || '';
      this.loading = false;
    }).catch((err) => {
      this.notificationService.error(this.errorHandler.getMensaje(err));
      this.loading = false;
      console.error(err);
    });
  }

  cargarPetroleras(): void {
    this.petroleraService.getAll().subscribe({
      next: (petroleras) => {
        this.petroleras = petroleras.filter(p => p.activa);
      },
      error: (err) => {
        console.error('Error al cargar petroleras', err);
      }
    });
  }

  nuevoTipoSolicitud(): void {
    this.editandoId = null;
    this.archivoSeleccionado = null;
    this.nombreArchivoMostrar = '';
    this.tipoSolicitudForm.reset({
      activa: true
    });
    this.mostrarFormulario = true;
  }

  editarTipoSolicitud(tipoSolicitud: TipoSolicitud): void {
    this.editandoId = tipoSolicitud.id!;
    this.editandoOrden = tipoSolicitud.orden ?? null;
    this.archivoSeleccionado = null;
    this.nombreArchivoMostrar = tipoSolicitud.nombreArchivoPlantilla || '';
    this.tipoSolicitudForm.patchValue({
      nombre: tipoSolicitud.nombre,
      codigo: tipoSolicitud.codigo,
      activa: tipoSolicitud.activa
    });
    this.mostrarFormulario = true;
  }

  onArchivoSeleccionado(archivo: File): void {
    this.archivoSeleccionado = archivo;
    this.nombreArchivoMostrar = archivo.name;
  }

  /** La zona de subida ya ha validado tipo y tamaño: aquí solo se avisa del motivo. */
  onArchivoRechazado(mensaje: string): void {
    this.notificationService.error(mensaje);
  }

  onSeleccionLimpiada(): void {
    this.archivoSeleccionado = null;
    this.nombreArchivoMostrar = '';
  }

  guardarTipoSolicitud(): void {
    if (this.tipoSolicitudForm.invalid) {
      this.tipoSolicitudForm.markAllAsTouched();
      return;
    }

    if (!this.petroleraId) {
      this.notificationService.error('Debe seleccionar una petrolera');
      return;
    }

    const tipoSolicitudData: TipoSolicitud = {
      ...this.tipoSolicitudForm.value,
      petroleraId: this.petroleraId,
      orden: this.editandoId ? (this.editandoOrden ?? undefined) : (this.tiposSolicitud.length + 1)
    };

    const operation = this.editandoId
      ? this.tipoSolicitudService.update(this.editandoId, tipoSolicitudData)
      : this.tipoSolicitudService.create(tipoSolicitudData);

    operation.subscribe({
      next: (tipoCreado) => {
        const mensaje = this.editandoId
          ? 'Tipo de solicitud actualizado correctamente'
          : 'Tipo de solicitud creado correctamente';
        this.notificationService.success(mensaje);

        // Si hay archivo PDF seleccionado, subirlo
        if (this.archivoSeleccionado && tipoCreado.id) {
          this.subirPlantillaPdf(tipoCreado.id);
        } else {
          this.mostrarFormulario = false;
          this.cargarTiposSolicitudDePetrolera(this.petroleraId!);
        }
      },
      error: (err) => {
        this.notificationService.error(this.errorHandler.getMensaje(err));
        console.error(err);
      }
    });
  }

  subirPlantillaPdf(id: string): void {
    if (!this.archivoSeleccionado) return;

    this.tipoSolicitudService.subirPlantillaPdf(id, this.archivoSeleccionado).subscribe({
      next: () => {
        this.notificationService.success('Plantilla PDF cargada correctamente');
        this.mostrarFormulario = false;
        this.archivoSeleccionado = null;
        if (this.petroleraId) {
          this.cargarTiposSolicitudDePetrolera(this.petroleraId);
        }
      },
      error: (err) => {
        this.notificationService.error(this.errorHandler.getMensaje(err));
        console.error(err);
      }
    });
  }

  eliminarPlantillaPdf(id: string): void {
    if (confirm('¿Está seguro de eliminar la plantilla PDF?')) {
      this.tipoSolicitudService.eliminarPlantillaPdf(id).subscribe({
        next: () => {
          this.notificationService.success('Plantilla PDF eliminada correctamente');
          if (this.petroleraId) {
            this.cargarTiposSolicitudDePetrolera(this.petroleraId);
          }
        },
        error: (err) => {
          this.notificationService.error(this.errorHandler.getMensaje(err));
          console.error(err);
        }
      });
    }
  }

  cancelarFormulario(): void {
    this.mostrarFormulario = false;
    this.editandoId = null;
    this.editandoOrden = null;
    this.archivoSeleccionado = null;
    this.nombreArchivoMostrar = '';
    this.tipoSolicitudForm.reset();
  }

  eliminarTipoSolicitud(id: string): void {
    if (confirm('¿Está seguro de eliminar este tipo de solicitud?')) {
      this.tipoSolicitudService.delete(id).subscribe({
        next: () => {
          this.notificationService.success('Tipo de solicitud eliminado correctamente');
          if (this.petroleraId) {
            this.cargarTiposSolicitudDePetrolera(this.petroleraId);
          }
        },
        error: (err) => {
          this.notificationService.error(this.errorHandler.getMensaje(err));
          console.error(err);
        }
      });
    }
  }

  previsualizarPlantillaPdf(id: string): void {
    this.tipoSolicitudService.descargarPlantillaPdfBlob(id).subscribe({
      next: (blob) => {
        if (this._blobUrlPdf) {
          URL.revokeObjectURL(this._blobUrlPdf);
        }
        this._blobUrlPdf = URL.createObjectURL(blob);
        this.urlPdfModal = this.sanitizer.bypassSecurityTrustResourceUrl(this._blobUrlPdf);
        this.mostrarModalPdf = true;
      },
      error: (err) => {
        this.notificationService.error(this.errorHandler.getMensaje(err));
        console.error(err);
      }
    });
  }

  cerrarModalPdf(): void {
    this.mostrarModalPdf = false;
    if (this._blobUrlPdf) {
      URL.revokeObjectURL(this._blobUrlPdf);
      this._blobUrlPdf = null;
    }
    this.urlPdfModal = null;
  }

  volver(): void {
    this.router.navigate(['/petroleras']);
  }

  get f() {
    return this.tipoSolicitudForm.controls;
  }
}
