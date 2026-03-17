import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { SolicitudContratoService } from '../../../services/solicitud-contrato.service';
import { TipoSolicitudService } from '../../../services/tipo-solicitud.service';
import { SocioService } from '../../../services/socio.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { EmpresaService } from '../../../services/empresa.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { SolicitudContrato, EstadoSolicitud, TipoSolicitudContrato } from '../../../models/solicitud-contrato.model';
import { PdfPreviewModal } from '../../shared/pdf-preview-modal/pdf-preview-modal';
import { PdfEditorModal } from '../pdf-editor-modal/pdf-editor-modal';
import { EmailLogs } from '../../solicitudes-tarjetas/email-logs/email-logs';

@Component({
  selector: 'app-contrato-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, PdfPreviewModal, PdfEditorModal, EmailLogs],
  templateUrl: './contrato-detail.html',
  styleUrl: './contrato-detail.css'
})
export class ContratoDetail implements OnInit {
  solicitud: SolicitudContrato | null = null;
  loading = false;
  procesando = false;
  selectedFile: File | null = null;

  // Referencias a los inputs de archivo
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  @ViewChild('fileInputFirmado') fileInputFirmado?: ElementRef<HTMLInputElement>;

  // Preview modal
  modalPreviewAbierto = false;
  pdfPreviewBlob: Blob | null = null;
  tituloPreview = '';
  tipoPdfActual: 'editable' | 'enviado' | 'firmado' | 'final' | null = null;

  // Editor de campos PDF
  modalEditorCamposAbierto = false;

  // Resolución petrolera
  mostrarFormRechazo = false;
  motivoRechazo = '';

  EstadoSolicitud = EstadoSolicitud;
  nombreTipoSolicitudPetrolera = '';
  nombreSocio = '';
  nombreEmpresa = '';
  nombrePetrolera = '';

  constructor(
    private solicitudService: SolicitudContratoService,
    private tipoSolicitudService: TipoSolicitudService,
    private socioService: SocioService,
    private petroleraService: PetroleraService,
    private empresaService: EmpresaService,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.cargarSolicitud(parseInt(id));
    }
  }

  cargarSolicitud(id: number): void {
    this.loading = true;
    this.solicitudService.obtenerPorId(id).subscribe({
      next: (solicitud) => {
        this.solicitud = solicitud;
        this.loading = false;
        // Cargar nombre del tipo de solicitud petrolera si existe
        if (solicitud.tipoSolicitudPetroleraId) {
          this.tipoSolicitudService.getById(String(solicitud.tipoSolicitudPetroleraId)).subscribe({
            next: (tipo) => {
              this.nombreTipoSolicitudPetrolera = tipo.nombre;
            },
            error: () => {}
          });
        } else if (solicitud.subtipoNombre) {
          // Para BAJA y CAMBIO_CONDICIONES, usar el subtipo heredado del contrato original
          this.nombreTipoSolicitudPetrolera = solicitud.subtipoNombre;
        }
        // Cargar nombre del socio
        this.socioService.getById(String(solicitud.socioId)).subscribe({
          next: (socio) => {
            this.nombreSocio = socio.nombre;
          },
          error: () => {}
        });
        // Cargar nombre de la petrolera
        this.petroleraService.getById(String(solicitud.petroleraId)).subscribe({
          next: (petrolera) => {
            this.nombrePetrolera = petrolera.nombre;
          },
          error: () => {}
        });
        // Cargar nombre de la empresa si existe
        if (solicitud.empresaId) {
          this.empresaService.getById(String(solicitud.empresaId)).subscribe({
            next: (empresa) => {
              this.nombreEmpresa = empresa.nombre;
            },
            error: () => {}
          });
        }
      },
      error: (err) => {
        this.loading = false;
        console.error('Error al cargar contrato', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
        this.router.navigate(['/contratos']);
      }
    });
  }

  descargarPdf(tipo: 'editable' | 'enviado' | 'firmado' | 'final'): void {
    if (!this.solicitud?.id) return;

    this.solicitudService.descargarPdf(this.solicitud.id, tipo).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${this.solicitud?.numeroSolicitud}_${tipo}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.notificationService.success('PDF descargado correctamente');
      },
      error: (err) => {
        console.error('Error al descargar PDF', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  descargarPlantillaOriginal(): void {
    if (!this.solicitud?.id) return;

    this.solicitudService.descargarPlantillaOriginal(this.solicitud.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${this.solicitud?.numeroSolicitud}_plantilla_original.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.notificationService.success('Plantilla descargada correctamente');
      },
      error: (err) => {
        console.error('Error al descargar plantilla', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  previsualizarPlantillaOriginal(): void {
    if (!this.solicitud?.id) return;

    this.tituloPreview = 'Plantilla Original';
    this.tipoPdfActual = null;

    this.solicitudService.descargarPlantillaOriginal(this.solicitud.id).subscribe({
      next: (blob) => {
        this.pdfPreviewBlob = blob;
        this.modalPreviewAbierto = true;
      },
      error: (err) => {
        console.error('Error al cargar plantilla para previsualización', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file && file.type === 'application/pdf') {
      this.selectedFile = file;
    } else {
      this.notificationService.error('Por favor seleccione un archivo PDF válido');
      this.selectedFile = null;
    }
  }

  guardarPdfEditado(): void {
    if (!this.solicitud?.id || !this.selectedFile) {
      this.notificationService.error('Debe seleccionar un archivo PDF');
      return;
    }

    this.procesando = true;
    this.solicitudService.guardarPdfEditado(this.solicitud.id, this.selectedFile).subscribe({
      next: () => {
        this.notificationService.success('PDF guardado correctamente');
        this.selectedFile = null;
        // Resetear el input de archivo
        if (this.fileInput?.nativeElement) {
          this.fileInput.nativeElement.value = '';
        }
        this.cargarSolicitud(this.solicitud!.id!);
        this.procesando = false;
      },
      error: (err) => {
        this.procesando = false;
        console.error('Error al guardar PDF', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  enviarASocio(): void {
    if (!this.solicitud?.id) return;

    if (!confirm('¿Está seguro de enviar la solicitud al socio? El PDF será aplanado (no editable).')) {
      return;
    }

    this.procesando = true;
    this.solicitudService.enviarASocio(this.solicitud.id).subscribe({
      next: (solicitudActualizada) => {
        this.solicitud = solicitudActualizada;
        this.procesando = false;
        this.notificationService.success('Solicitud enviada al socio correctamente');
      },
      error: (err) => {
        this.procesando = false;
        console.error('Error al enviar al socio', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  subirPdfFirmado(): void {
    if (!this.solicitud?.id || !this.selectedFile) {
      this.notificationService.error('Debe seleccionar un archivo PDF firmado');
      return;
    }

    this.procesando = true;
    this.solicitudService.subirPdfFirmado(this.solicitud.id, this.selectedFile).subscribe({
      next: (solicitudActualizada) => {
        this.solicitud = solicitudActualizada;
        this.selectedFile = null;
        // Resetear el input de archivo
        if (this.fileInputFirmado?.nativeElement) {
          this.fileInputFirmado.nativeElement.value = '';
        }
        this.procesando = false;
        this.notificationService.success('PDF firmado recibido correctamente');
      },
      error: (err) => {
        this.procesando = false;
        console.error('Error al subir PDF firmado', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  enviarAPetrolera(): void {
    if (!this.solicitud?.id) return;

    if (!confirm('¿Está seguro de enviar la solicitud a la petrolera?')) {
      return;
    }

    this.procesando = true;
    this.solicitudService.enviarAPetrolera(this.solicitud.id).subscribe({
      next: (solicitudActualizada) => {
        this.solicitud = solicitudActualizada;
        this.procesando = false;
        this.notificationService.success('Solicitud enviada a la petrolera correctamente');
      },
      error: (err) => {
        this.procesando = false;
        console.error('Error al enviar a petrolera', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  aceptarFirmaSocio(): void {
    if (!this.solicitud?.id) return;

    if (!confirm('¿Está seguro de aceptar la firma del socio? Se avanzará al siguiente paso.')) {
      return;
    }

    this.procesando = true;
    this.solicitudService.aceptarFirmaSocio(this.solicitud.id).subscribe({
      next: (solicitudActualizada) => {
        this.solicitud = solicitudActualizada;
        this.procesando = false;
        this.notificationService.success('Firma del socio aceptada correctamente');
      },
      error: (err) => {
        this.procesando = false;
        console.error('Error al aceptar firma', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  aceptarPorPetrolera(): void {
    if (!this.solicitud?.id) return;

    if (!confirm('¿Está seguro de aceptar la solicitud? Se creará el contrato correspondiente.')) {
      return;
    }

    this.procesando = true;
    this.solicitudService.aceptarPorPetrolera(this.solicitud.id).subscribe({
      next: (solicitudActualizada) => {
        this.solicitud = solicitudActualizada;
        this.procesando = false;
        this.notificationService.success('Solicitud aceptada por la petrolera');
      },
      error: (err) => {
        this.procesando = false;
        console.error('Error al aceptar por petrolera', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  rechazarPorPetrolera(): void {
    if (!this.solicitud?.id) return;

    if (!confirm('¿Está seguro de rechazar la solicitud? Esta acción es definitiva.')) {
      return;
    }

    this.procesando = true;
    this.solicitudService.rechazarPorPetrolera(this.solicitud.id, this.motivoRechazo).subscribe({
      next: (solicitudActualizada) => {
        this.solicitud = solicitudActualizada;
        this.procesando = false;
        this.mostrarFormRechazo = false;
        this.motivoRechazo = '';
        this.notificationService.success('Solicitud rechazada por la petrolera');
      },
      error: (err) => {
        this.procesando = false;
        console.error('Error al rechazar por petrolera', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  getEstadoClass(estado: EstadoSolicitud): string {
    const clases: Record<EstadoSolicitud, string> = {
      [EstadoSolicitud.BORRADOR]: 'badge-warning',
      [EstadoSolicitud.ENVIADO_SOCIO]: 'badge-info',
      [EstadoSolicitud.FIRMADO_SOCIO]: 'badge-primary',
      [EstadoSolicitud.ENVIADO_PETROLERA]: 'badge-warning',
      [EstadoSolicitud.ACEPTADA_PETROLERA]: 'badge-success',
      [EstadoSolicitud.RECHAZADA_PETROLERA]: 'badge-danger'
    };
    return clases[estado] || 'badge-secondary';
  }

  getEstadoTexto(estado: EstadoSolicitud): string {
    const textos: Record<EstadoSolicitud, string> = {
      [EstadoSolicitud.BORRADOR]: 'Borrador',
      [EstadoSolicitud.ENVIADO_SOCIO]: 'Enviado a Socio',
      [EstadoSolicitud.FIRMADO_SOCIO]: 'Firmado por Socio',
      [EstadoSolicitud.ENVIADO_PETROLERA]: 'Enviado a Petrolera',
      [EstadoSolicitud.ACEPTADA_PETROLERA]: 'Aceptada por Petrolera',
      [EstadoSolicitud.RECHAZADA_PETROLERA]: 'Rechazada por Petrolera'
    };
    return textos[estado] || estado;
  }

  getTipoSolicitudTexto(tipo: TipoSolicitudContrato): string {
    const textos: Record<TipoSolicitudContrato, string> = {
      [TipoSolicitudContrato.NUEVO]: 'Nuevo Contrato',
      [TipoSolicitudContrato.BAJA]: 'Baja de Contrato',
      [TipoSolicitudContrato.CAMBIO_CONDICIONES]: 'Cambio de Condiciones'
    };
    return textos[tipo] || tipo;
  }

  getTipoSolicitudClass(tipo: TipoSolicitudContrato): string {
    const clases: Record<TipoSolicitudContrato, string> = {
      [TipoSolicitudContrato.NUEVO]: 'badge-success',
      [TipoSolicitudContrato.BAJA]: 'badge-danger',
      [TipoSolicitudContrato.CAMBIO_CONDICIONES]: 'badge-warning'
    };
    return clases[tipo] || 'badge-secondary';
  }

  puedeEditarPdf(): boolean {
    return this.solicitud?.estado === EstadoSolicitud.BORRADOR;
  }

  puedeEnviarASocio(): boolean {
    // Solo permitir si hay un PDF editado subido (no solo la plantilla original)
    return this.solicitud?.estado === EstadoSolicitud.BORRADOR &&
           !!this.solicitud.rutaPdfEditable &&
           !!this.solicitud.nombrePdfEditable &&
           this.solicitud.nombrePdfEditable !== 'editable.pdf';
  }

  puedeSubirFirmado(): boolean {
    return this.solicitud?.estado === EstadoSolicitud.ENVIADO_SOCIO;
  }

  puedeEnviarAPetrolera(): boolean {
    return this.solicitud?.estado === EstadoSolicitud.FIRMADO_SOCIO;
  }

  abrirEditorCampos(): void {
    this.modalEditorCamposAbierto = true;
  }

  onEditorCamposCerrado(): void {
    this.modalEditorCamposAbierto = false;
  }

  onCamposGuardados(): void {
    this.modalEditorCamposAbierto = false;
    if (this.solicitud?.id) {
      this.cargarSolicitud(this.solicitud.id);
    }
  }

  volver(): void {
    this.router.navigate(['/contratos']);
  }

  previsualizarPdf(tipo: 'editable' | 'enviado' | 'firmado' | 'final'): void {
    if (!this.solicitud?.id) return;

    const titulos = {
      editable: 'PDF Editable',
      enviado: 'PDF Enviado al Socio',
      firmado: 'PDF Firmado por el Socio',
      final: 'PDF Final'
    };

    this.tituloPreview = titulos[tipo];
    this.tipoPdfActual = tipo;

    this.solicitudService.descargarPdf(this.solicitud.id, tipo).subscribe({
      next: (blob) => {
        this.pdfPreviewBlob = blob;
        this.modalPreviewAbierto = true;
      },
      error: (err) => {
        console.error('Error al cargar PDF para previsualización', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  cerrarModalPreview(): void {
    this.modalPreviewAbierto = false;
    this.pdfPreviewBlob = null;
    this.tituloPreview = '';
    this.tipoPdfActual = null;
  }

  // Determinar si mostrar botón de descargar en el modal según el tipo de PDF
  get mostrarBotonDescargar(): boolean {
    // Solo mostrar botón de descargar para el PDF firmado
    return this.tipoPdfActual === 'firmado';
  }

  // Extraer el nombre del archivo de una ruta
  obtenerNombreArchivo(ruta?: string): string {
    if (!ruta) return '';
    const partes = ruta.split(/[\\/]/);
    return partes[partes.length - 1];
  }
}
