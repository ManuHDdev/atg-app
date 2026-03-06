import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { PetroleraService } from '../../../services/petrolera.service';
import { PlantillaEmailService } from '../../../services/plantilla-email.service';
import { Petrolera } from '../../../models/petrolera.model';
import { PlantillaEmail, TipoEventoEmail, VARIABLES_POR_TIPO } from '../../../models/plantilla-email.model';

@Component({
  selector: 'app-petrolera-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './petrolera-form.html',
  styleUrl: './petrolera-form.css'
})
export class PetroleraForm implements OnInit {
  petroleraForm: FormGroup;
  isEditMode = false;
  petroleraId: string | null = null;
  loading = false;
  error: string | null = null;

  // Días de la semana para envío de créditos
  readonly diasSemana = [
    { value: 'LUNES', label: 'Lunes' },
    { value: 'MARTES', label: 'Martes' },
    { value: 'MIERCOLES', label: 'Miércoles' },
    { value: 'JUEVES', label: 'Jueves' },
    { value: 'VIERNES', label: 'Viernes' },
    { value: 'SABADO', label: 'Sábado' },
    { value: 'DOMINGO', label: 'Domingo' }
  ];
  diasSeleccionados: string[] = [];

  // Plantillas de correo de contratos
  plantillasContrato: PlantillaEmail[] = [];
  loadingPlantillas = false;
  plantillaEditando: PlantillaEmail | null = null;
  mostrarFormPlantilla = false;

  // Plantillas de correo de créditos
  plantillasCredito: PlantillaEmail[] = [];
  loadingPlantillasCredito = false;
  plantillaCreditoEditando: PlantillaEmail | null = null;
  mostrarFormPlantillaCredito = false;

  // Tipos de plantilla de contratos que se gestionan desde aquí
  tiposPlantillaContrato = [
    { value: TipoEventoEmail.CONTRATO_PETROLERA, label: 'Contrato: Email a Petrolera' },
    { value: TipoEventoEmail.NOTIF_SOCIO_CONTRATO_CREADO, label: 'Contrato: Notif. Socio - Solicitud Registrada' },
    { value: TipoEventoEmail.NOTIF_SOCIO_CONTRATO_ENVIADO, label: 'Contrato: Notif. Socio - Contrato Enviado' },
    { value: TipoEventoEmail.NOTIF_SOCIO_CONTRATO_RESULTADO, label: 'Contrato: Notif. Socio - Resultado' },
    { value: TipoEventoEmail.ALTA_DISPOSITIVO, label: 'Dispositivo: Alta' },
    { value: TipoEventoEmail.SOLICITUD_CREDITO_DISPOSITIVO, label: 'Dispositivo: Solicitud de Crédito' },
    { value: TipoEventoEmail.BAJA_DISPOSITIVO, label: 'Dispositivo: Baja' },
    { value: TipoEventoEmail.CAMBIO_MATRICULA, label: 'Dispositivo: Cambio de Matrícula' },
    { value: TipoEventoEmail.NOTIF_SOCIO_DISP_CREADO, label: 'Dispositivo: Notif. Solicitud Registrada' },
    { value: TipoEventoEmail.NOTIF_SOCIO_DISP_ENVIADO, label: 'Dispositivo: Notif. Enviado a Petrolera' },
    { value: TipoEventoEmail.NOTIF_SOCIO_DISP_RESULTADO, label: 'Dispositivo: Notif. Resultado' }
  ];

  // Tipos de plantilla de créditos
  tiposPlantillaCredito = [
    { value: TipoEventoEmail.SOLICITUD_CREDITO, label: 'Crédito: Solicitud (a Petrolera)' },
    { value: TipoEventoEmail.AMPLIACION_CREDITO, label: 'Crédito: Ampliación (a Petrolera)' },
    { value: TipoEventoEmail.DEVOLUCION_AVAL, label: 'Crédito: Devolución de Aval (a Petrolera)' },
    { value: TipoEventoEmail.NOTIF_SOCIO_CREADO, label: 'Crédito: Notif. Socio - Solicitud Registrada' },
    { value: TipoEventoEmail.NOTIF_SOCIO_ENVIADO, label: 'Crédito: Notif. Socio - Enviado a Petrolera' },
    { value: TipoEventoEmail.NOTIF_SOCIO_RESULTADO, label: 'Crédito: Notif. Socio - Resultado' }
  ];

  nuevaPlantilla = {
    tipoEvento: '' as string,
    asunto: '',
    cuerpo: '',
    activa: true
  };

  nuevaPlantillaCredito = {
    tipoEvento: '' as string,
    asunto: '',
    cuerpo: '',
    activa: true
  };

  constructor(
    private fb: FormBuilder,
    private petroleraService: PetroleraService,
    private plantillaEmailService: PlantillaEmailService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.petroleraForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      activa: [true],
      email: ['', [Validators.email, Validators.maxLength(255)]],
      diasEnvioCreditos: [null]
    });
  }

  ngOnInit(): void {
    this.petroleraId = this.route.snapshot.paramMap.get('id');
    if (this.petroleraId) {
      this.isEditMode = true;
      this.cargarPetrolera();
      this.cargarPlantillasContrato();
    }
  }

  cargarPetrolera(): void {
    if (!this.petroleraId) return;

    this.loading = true;
    this.petroleraService.getById(this.petroleraId).subscribe({
      next: (petrolera) => {
        this.petroleraForm.patchValue(petrolera);
        this.diasSeleccionados = petrolera.diasEnvioCreditos
          ? petrolera.diasEnvioCreditos.split(',').map(d => d.trim()).filter(d => d)
          : [];
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar la petrolera';
        this.loading = false;
        console.error(err);
      }
    });
  }

  cargarPlantillasContrato(): void {
    if (!this.petroleraId) return;

    this.loadingPlantillas = true;
    this.loadingPlantillasCredito = true;
    this.plantillaEmailService.obtenerPorPetrolera(Number(this.petroleraId)).subscribe({
      next: (plantillas) => {
        const tiposContrato = this.tiposPlantillaContrato.map(t => t.value);
        const tiposCredito = this.tiposPlantillaCredito.map(t => t.value);
        this.plantillasContrato = plantillas.filter(p =>
          tiposContrato.includes(p.tipoEvento as TipoEventoEmail)
        );
        this.plantillasCredito = plantillas.filter(p =>
          tiposCredito.includes(p.tipoEvento as TipoEventoEmail)
        );
        this.loadingPlantillas = false;
        this.loadingPlantillasCredito = false;
      },
      error: (err) => {
        console.error('Error al cargar plantillas:', err);
        this.loadingPlantillas = false;
        this.loadingPlantillasCredito = false;
      }
    });
  }

  obtenerLabelTipo(tipo: string): string {
    const found = this.tiposPlantillaContrato.find(t => t.value === tipo);
    return found ? found.label : tipo;
  }

  obtenerLabelTipoCredito(tipo: string): string {
    const found = this.tiposPlantillaCredito.find(t => t.value === tipo);
    return found ? found.label : tipo;
  }

  getVariablesDisponibles(tipo: string): string[] {
    return VARIABLES_POR_TIPO[tipo as TipoEventoEmail] || [];
  }

  abrirFormPlantilla(plantilla?: PlantillaEmail): void {
    if (plantilla) {
      this.plantillaEditando = plantilla;
      this.nuevaPlantilla = {
        tipoEvento: plantilla.tipoEvento,
        asunto: plantilla.asunto,
        cuerpo: plantilla.cuerpo,
        activa: plantilla.activa
      };
    } else {
      this.plantillaEditando = null;
      this.nuevaPlantilla = {
        tipoEvento: '',
        asunto: '',
        cuerpo: '',
        activa: true
      };
    }
    this.mostrarFormPlantilla = true;
  }

  cerrarFormPlantilla(): void {
    this.mostrarFormPlantilla = false;
    this.plantillaEditando = null;
  }

  insertarVariable(variable: string): void {
    this.nuevaPlantilla.cuerpo += variable;
  }

  guardarPlantilla(): void {
    if (!this.petroleraId || !this.nuevaPlantilla.tipoEvento || !this.nuevaPlantilla.asunto.trim() || !this.nuevaPlantilla.cuerpo.trim()) {
      return;
    }

    const dto = {
      nombre: '',
      asunto: this.nuevaPlantilla.asunto,
      cuerpo: this.nuevaPlantilla.cuerpo,
      tipoEvento: this.nuevaPlantilla.tipoEvento as TipoEventoEmail,
      activa: this.nuevaPlantilla.activa,
      petroleraId: Number(this.petroleraId)
    };

    const operacion = this.plantillaEditando?.id
      ? this.plantillaEmailService.actualizar(this.plantillaEditando.id, dto)
      : this.plantillaEmailService.crear(dto);

    operacion.subscribe({
      next: () => {
        this.cerrarFormPlantilla();
        this.cargarPlantillasContrato();
      },
      error: (err) => {
        console.error('Error al guardar plantilla:', err);
        alert('Error al guardar la plantilla');
      }
    });
  }

  eliminarPlantilla(id: number): void {
    if (confirm('¿Está seguro de eliminar esta plantilla?')) {
      this.plantillaEmailService.eliminar(id).subscribe({
        next: () => {
          this.cargarPlantillasContrato();
        },
        error: (err) => {
          console.error('Error al eliminar plantilla:', err);
          alert('Error al eliminar la plantilla');
        }
      });
    }
  }

  abrirFormPlantillaCredito(plantilla?: PlantillaEmail): void {
    if (plantilla) {
      this.plantillaCreditoEditando = plantilla;
      this.nuevaPlantillaCredito = {
        tipoEvento: plantilla.tipoEvento,
        asunto: plantilla.asunto,
        cuerpo: plantilla.cuerpo,
        activa: plantilla.activa
      };
    } else {
      this.plantillaCreditoEditando = null;
      this.nuevaPlantillaCredito = {
        tipoEvento: '',
        asunto: '',
        cuerpo: '',
        activa: true
      };
    }
    this.mostrarFormPlantillaCredito = true;
  }

  cerrarFormPlantillaCredito(): void {
    this.mostrarFormPlantillaCredito = false;
    this.plantillaCreditoEditando = null;
  }

  insertarVariableCredito(variable: string): void {
    this.nuevaPlantillaCredito.cuerpo += variable;
  }

  guardarPlantillaCredito(): void {
    if (!this.petroleraId || !this.nuevaPlantillaCredito.tipoEvento || !this.nuevaPlantillaCredito.asunto.trim() || !this.nuevaPlantillaCredito.cuerpo.trim()) {
      return;
    }

    const dto = {
      nombre: '',
      asunto: this.nuevaPlantillaCredito.asunto,
      cuerpo: this.nuevaPlantillaCredito.cuerpo,
      tipoEvento: this.nuevaPlantillaCredito.tipoEvento as TipoEventoEmail,
      activa: this.nuevaPlantillaCredito.activa,
      petroleraId: Number(this.petroleraId)
    };

    const operacion = this.plantillaCreditoEditando?.id
      ? this.plantillaEmailService.actualizar(this.plantillaCreditoEditando.id, dto)
      : this.plantillaEmailService.crear(dto);

    operacion.subscribe({
      next: () => {
        this.cerrarFormPlantillaCredito();
        this.cargarPlantillasContrato();
      },
      error: (err) => {
        console.error('Error al guardar plantilla de crédito:', err);
        alert('Error al guardar la plantilla');
      }
    });
  }

  eliminarPlantillaCredito(id: number): void {
    if (confirm('¿Está seguro de eliminar esta plantilla?')) {
      this.plantillaEmailService.eliminar(id).subscribe({
        next: () => {
          this.cargarPlantillasContrato();
        },
        error: (err) => {
          console.error('Error al eliminar plantilla de crédito:', err);
          alert('Error al eliminar la plantilla');
        }
      });
    }
  }

  isDiaSeleccionado(dia: string): boolean {
    return this.diasSeleccionados.includes(dia);
  }

  toggleDia(dia: string): void {
    const idx = this.diasSeleccionados.indexOf(dia);
    if (idx >= 0) {
      this.diasSeleccionados.splice(idx, 1);
    } else {
      this.diasSeleccionados.push(dia);
    }
    const valor = this.diasSeleccionados.length > 0 ? this.diasSeleccionados.join(',') : null;
    this.petroleraForm.patchValue({ diasEnvioCreditos: valor });
  }

  onSubmit(): void {
    if (this.petroleraForm.invalid) {
      this.petroleraForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const petroleraData: Petrolera = this.petroleraForm.value;

    const operation = this.isEditMode && this.petroleraId
      ? this.petroleraService.update(this.petroleraId, petroleraData)
      : this.petroleraService.create(petroleraData);

    operation.subscribe({
      next: () => {
        this.router.navigate(['/petroleras']);
      },
      error: (err) => {
        this.error = 'Error al guardar la petrolera';
        this.loading = false;
        console.error(err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/petroleras']);
  }

  get f() {
    return this.petroleraForm.controls;
  }
}
