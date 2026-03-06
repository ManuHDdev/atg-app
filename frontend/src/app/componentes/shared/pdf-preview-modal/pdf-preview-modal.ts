import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-pdf-preview-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pdf-preview-modal.html',
  styleUrl: './pdf-preview-modal.css'
})
export class PdfPreviewModal {
  @Input() isOpen = false;
  @Input() titulo = 'Vista previa del PDF';
  @Input() pdfBlob: Blob | null = null;
  @Input() mostrarBotonDescargar = true; // Por defecto muestra el botón
  @Output() close = new EventEmitter<void>();

  pdfPreviewUrl: SafeResourceUrl | null = null;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnChanges(): void {
    if (this.isOpen && this.pdfBlob && !this.pdfPreviewUrl) {
      this.cargarPdf();
    }
  }

  cargarPdf(): void {
    if (this.pdfBlob) {
      const url = URL.createObjectURL(this.pdfBlob);
      this.pdfPreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    }
  }

  descargarPdf(): void {
    if (!this.pdfPreviewUrl || !this.pdfBlob) return;

    const url = (this.pdfPreviewUrl as any).changingThisBreaksApplicationSecurity;
    if (url) {
      const link = document.createElement('a');
      link.href = url;
      link.download = 'contrato.pdf';
      link.click();
    }
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

    this.close.emit();
  }
}
