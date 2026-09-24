import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PlantillaEmailService } from '../../../services/plantilla-email.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { Petrolera } from '../../../models/petrolera.model';
import { PlantillaEmail, TipoEventoEmail, CrearPlantillaEmailDTO, VARIABLES_POR_TIPO, TIPOS_EVENTO_EMAIL } from '../../../models/plantilla-email.model';

@Component({
  selector: 'app-plantilla-email-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './plantilla-email-form.html',
  styleUrls: ['./plantilla-email-form.css']
})
export class PlantillaEmailForm implements OnInit {
  plantilla: CrearPlantillaEmailDTO = {
    nombre: '',
    asunto: '',
    cuerpo: '',
    tipoEvento: TipoEventoEmail.SOLICITUD_CREDITO,
    activa: true,
    petroleraId: 0
  };

  petroleras: Petrolera[] = [];

  plantillaId?: number;
  isEditMode = false;
  loading = false;
  error = '';

  tiposEvento = TIPOS_EVENTO_EMAIL;

  mostrarPreview = false;
  previewHtml = '';

  constructor(
    private plantillaEmailService: PlantillaEmailService,
    private petroleraService: PetroleraService,
    private route: ActivatedRoute,
    private router: Router,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.cargarPetroleras();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.plantillaId = parseInt(id);
      this.isEditMode = true;
      this.cargarPlantilla();
    }
  }

  cargarPetroleras(): void {
    this.petroleraService.listar().subscribe({
      next: (data) => {
        this.petroleras = data.filter(p => p.activa);
      },
      error: (error) => {
        console.error('Error al cargar petroleras:', error);
      }
    });
  }

  cargarPlantilla(): void {
    if (!this.plantillaId) return;

    this.loading = true;
    this.plantillaEmailService.obtenerPorId(this.plantillaId).subscribe({
      next: (plantilla) => {
        this.plantilla = {
          nombre: plantilla.nombre,
          asunto: plantilla.asunto,
          cuerpo: plantilla.cuerpo,
          tipoEvento: plantilla.tipoEvento,
          activa: plantilla.activa,
          petroleraId: plantilla.petroleraId
        };
        this.loading = false;
      },
      error: (error) => {
        console.error('Error al cargar plantilla:', error);
        this.error = this.errorHandler.getMensaje(error);
        this.loading = false;
      }
    });
  }

  get variablesDisponibles(): string[] {
    return VARIABLES_POR_TIPO[this.plantilla.tipoEvento] || [];
  }

  insertarVariable(variable: string): void {
    // Insertar en el campo cuerpo en la posición del cursor
    const textarea = document.getElementById('cuerpo') as HTMLTextAreaElement;
    if (textarea) {
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const text = this.plantilla.cuerpo;

      this.plantilla.cuerpo = text.substring(0, start) + variable + text.substring(end);

      // Restaurar el foco y la posición del cursor
      setTimeout(() => {
        textarea.focus();
        textarea.setSelectionRange(start + variable.length, start + variable.length);
      }, 0);
    } else {
      // Si no hay textarea, simplemente agregar al final
      this.plantilla.cuerpo += variable;
    }
  }

  insertarVariableEnAsunto(variable: string): void {
    this.plantilla.asunto += variable;
  }

  generarPreview(): void {
    this.previewHtml = this.plantilla.cuerpo;

    // Reemplazar variables con valores de ejemplo
    const ejemplos: Record<string, string> = {
      '{{socio_nombre}}': 'Juan Pérez',
      '{{socio_email}}': 'juan.perez@email.com',
      '{{socio_telefono}}': '612 345 678',
      '{{socio_numero}}': 'SOC-001',
      '{{empresa_nombre}}': 'Transportes Ejemplo S.L.',
      '{{empresa_cif}}': 'B12345678',
      '{{empresa_email}}': 'info@transportes.com',
      '{{petrolera_nombre}}': 'Repsol',
      '{{tipo_credito}}': 'SOLICITUD_CREDITO',
      '{{monto}}': '5.000,00 €',
      '{{observaciones}}': 'Ninguna observación adicional',
      '{{fecha_solicitud}}': '10/02/2026',
      '{{estado}}': 'APROBADO',
      '{{respuesta_petrolera}}': 'Crédito concedido sin incidencias',
      '{{socio_nif}}': '12345678A',
      '{{matricula}}': '1234ABC',
      '{{agrupacion}}': 'Grupo A',
      '{{numero_contrato}}': 'CTR-2026-00001',
      '{{fecha_actual}}': '10/02/2026',
      '{{numero_solicitud}}': 'SOL-2026-00001',
      '{{tipo_solicitud}}': 'ALTA_DISPOSITIVO',
      '{{matricula_destino}}': '5678XYZ'
    };

    Object.keys(ejemplos).forEach(variable => {
      this.previewHtml = this.previewHtml.replace(new RegExp(variable, 'g'), ejemplos[variable]);
    });

    this.mostrarPreview = true;
  }

  cerrarPreview(): void {
    this.mostrarPreview = false;
  }

  guardar(): void {
    // Validaciones
    if (!this.plantilla.petroleraId) {
      this.error = 'La petrolera es obligatoria';
      return;
    }

    if (!this.plantilla.asunto.trim()) {
      this.error = 'El asunto es obligatorio';
      return;
    }

    if (!this.plantilla.cuerpo.trim()) {
      this.error = 'El cuerpo del email es obligatorio';
      return;
    }

    this.loading = true;
    this.error = '';

    const operacion = this.isEditMode && this.plantillaId
      ? this.plantillaEmailService.actualizar(this.plantillaId, this.plantilla)
      : this.plantillaEmailService.crear(this.plantilla);

    operacion.subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/plantillas-email']);
      },
      error: (error) => {
        console.error('Error al guardar plantilla:', error);
        this.error = this.errorHandler.getMensaje(error);
        this.loading = false;
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/plantillas-email']);
  }

  obtenerLabelTipoEvento(tipo: TipoEventoEmail): string {
    const tipoObj = this.tiposEvento.find(t => t.value === tipo);
    return tipoObj ? tipoObj.label : tipo;
  }
}
