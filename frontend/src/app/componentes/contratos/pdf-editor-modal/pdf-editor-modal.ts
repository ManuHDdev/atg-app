import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PdfEditorService, CampoPdf } from '../../../services/pdf-editor.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { ZonaSoltarArchivo } from '../../shared/zona-soltar-archivo/zona-soltar-archivo';

@Component({
  selector: 'app-pdf-editor-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ZonaSoltarArchivo],
  templateUrl: './pdf-editor-modal.html',
  styleUrl: './pdf-editor-modal.css'
})
export class PdfEditorModal implements OnInit {
  @Input() solicitudId: number | null = null;
  @Input() tipoSolicitudId: number | null = null; // Para modo plantilla (antes de crear contrato)
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<File | null>(); // Emite el archivo PDF editado o null
  @Output() fieldsSaved = new EventEmitter<void>();  // Emite cuando los campos se guardaron en servidor

  campos: CampoPdf[] = [];
  valoresCampos: Record<string, string> = {};
  loading = false;
  saving = false;
  guardandoCampos = false;
  pdfPreviewUrl: SafeResourceUrl | null = null;
  archivoEditado: File | null = null;

  constructor(
    private pdfEditorService: PdfEditorService,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    if (this.isOpen) {
      this.cargarCamposPdf();
    }
  }

  ngOnChanges(): void {
    if (this.isOpen && this.campos.length === 0) {
      this.cargarCamposPdf();
    }
  }

  cargarCamposPdf(): void {
    this.loading = true;

    // Determinar si cargamos desde una solicitud existente o desde una plantilla
    const camposObservable = this.solicitudId
      ? this.pdfEditorService.obtenerCamposPdf(this.solicitudId)
      : this.tipoSolicitudId
      ? this.pdfEditorService.obtenerCamposPlantilla(this.tipoSolicitudId)
      : null;

    const previewObservable = this.solicitudId
      ? this.pdfEditorService.obtenerPdfBase64(this.solicitudId)
      : this.tipoSolicitudId
      ? this.pdfEditorService.obtenerPlantillaBase64(this.tipoSolicitudId)
      : null;

    if (!camposObservable || !previewObservable) {
      this.notificationService.error('No se especificó solicitud ni plantilla');
      this.loading = false;
      return;
    }

    // Cargar campos y preview en paralelo
    Promise.all([
      camposObservable.toPromise(),
      previewObservable.toPromise()
    ]).then(([formulario, pdfResponse]) => {
      // Cargar campos
      if (formulario) {
        this.campos = formulario.campos;
        this.campos.forEach(campo => {
          this.valoresCampos[campo.nombre] = campo.valor || '';
        });
      }

      // Cargar preview del PDF
      if (pdfResponse) {
        const blob = this.base64ToBlob(pdfResponse.base64, 'application/pdf');
        const url = URL.createObjectURL(blob);
        this.pdfPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
      }

      this.loading = false;
    }).catch((err) => {
      console.error('Error al cargar PDF', err);
      this.notificationService.error(this.errorHandler.getMensaje(err));
      this.loading = false;
    });
  }


  descargarPdf(): void {
    if (!this.pdfPreviewUrl) return;

    // Obtener el blob URL y descargar el PDF
    const url = (this.pdfPreviewUrl as any).changingThisBreaksApplicationSecurity;
    if (url) {
      const link = document.createElement('a');
      link.href = url;
      link.download = 'plantilla-contrato.pdf';
      link.click();
      this.notificationService.success('PDF descargado. Edítelo y súbalo cuando termine.');
    }
  }

  onArchivoSeleccionado(archivo: File): void {
    this.archivoEditado = archivo;
    this.notificationService.success(`Archivo "${archivo.name}" seleccionado`);
  }

  /** La zona de subida ya ha validado tipo y tamaño (PDF, máximo 50 MB). */
  onArchivoRechazado(mensaje: string): void {
    this.notificationService.error(mensaje);
  }

  guardarCambios(): void {
    this.saving = true;

    // Emitir el archivo editado (o null si no se subió ninguno)
    // El componente padre decidirá qué hacer con él
    this.saved.emit(this.archivoEditado);
    this.saving = false;
    this.cerrar();
  }

  guardarCamposRellenados(): void {
    if (!this.solicitudId) return;

    this.guardandoCampos = true;
    this.pdfEditorService.rellenarCamposPdf(this.solicitudId, this.valoresCampos).subscribe({
      next: (blob) => {
        // Actualizar el preview con el PDF ya rellenado
        const url = URL.createObjectURL(blob);
        if (this.pdfPreviewUrl) {
          const oldUrl = (this.pdfPreviewUrl as any).changingThisBreaksApplicationSecurity;
          if (oldUrl) URL.revokeObjectURL(oldUrl);
        }
        this.pdfPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.guardandoCampos = false;
        this.notificationService.success('Campos guardados en el PDF correctamente');
        this.fieldsSaved.emit();
        this.cerrar();
      },
      error: (err) => {
        console.error('Error al guardar campos en el PDF', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
        this.guardandoCampos = false;
      }
    });
  }

  onCheckboxChange(nombre: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.valoresCampos[nombre] = input.checked ? 'Yes' : 'Off';
  }

  cerrar(): void {
    // Limpiar el objeto URL del PDF
    if (this.pdfPreviewUrl) {
      const url = (this.pdfPreviewUrl as any).changingThisBreaksApplicationSecurity;
      if (url) {
        URL.revokeObjectURL(url);
      }
      this.pdfPreviewUrl = null;
    }

    // Resetear el estado completo del componente
    this.campos = [];
    this.valoresCampos = {};
    this.loading = false;
    this.saving = false;
    this.guardandoCampos = false;
    this.archivoEditado = null;

    this.close.emit();
  }

  private base64ToBlob(base64: string, contentType: string): Blob {
    const byteCharacters = atob(base64);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    return new Blob([byteArray], { type: contentType });
  }

  getTipoCampo(campo: CampoPdf): 'text' | 'checkbox' | 'select' {
    const tipoDetallado = campo.tipoDetallado?.toLowerCase();
    const tipoCrudo = campo.tipo?.toLowerCase();

    if (tipoDetallado === 'checkbox' || tipoCrudo === 'btn') return 'checkbox';
    if ((tipoDetallado === 'choice' || tipoCrudo === 'ch') && campo.opciones?.length) return 'select';
    return 'text';
  }

  tieneCamposEditables(): boolean {
    return this.solicitudId !== null && this.campos.length > 0;
  }
}
