import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { SolicitudContratoService } from '../../../services/solicitud-contrato.service';
import { TipoContratoService } from '../../../services/tipo-contrato.service';
import { TipoSolicitudService } from '../../../services/tipo-solicitud.service';
import { PetroleraService } from '../../../services/petrolera.service';
import { SocioService } from '../../../services/socio.service';
import { EmpresaService } from '../../../services/empresa.service';
import { NotificationService } from '../../../services/notification.service';
import { ErrorHandlerService } from '../../../services/error-handler.service';
import { TipoContrato } from '../../../models/tipo-contrato.model';
import { TipoSolicitud } from '../../../models/tipo-solicitud.model';
import { Petrolera, petroleraPermite } from '../../../models/petrolera.model';
import { Socio } from '../../../models/socio.model';
import { Empresa } from '../../../models/empresa.model';
import { CrearSolicitudDTO, TipoSolicitudContrato } from '../../../models/solicitud-contrato.model';
import { ContratoSocio } from '../../../models/contrato-socio.model';
import { ContratoSocioService } from '../../../services/contrato-socio.service';
import { SocioAutocomplete } from '../../shared/socio-autocomplete/socio-autocomplete';

@Component({
  selector: 'app-contrato-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SocioAutocomplete],
  templateUrl: './contrato-form.html',
  styleUrl: './contrato-form.css'
})
export class ContratoForm implements OnInit {
  contratoForm: FormGroup;
  tiposContrato: TipoContrato[] = [];
  tiposSolicitud: TipoSolicitud[] = [];
  petroleras: Petrolera[] = [];
  petrolerasFiltradas: Petrolera[] = [];
  socios: Socio[] = [];
  empresas: Empresa[] = [];
  empresasFiltradas: Empresa[] = [];
  contratosActivos: ContratoSocio[] = [];
  selectedSocio: Socio | null = null;
  loading = false;
  mostrarSubsecciones = false;

  // Exponer enums al template
  TipoSolicitudContrato = TipoSolicitudContrato;

  // PDF editado (si aplica)
  pdfEditado: File | null = null;

  constructor(
    private fb: FormBuilder,
    private solicitudService: SolicitudContratoService,
    private tipoContratoService: TipoContratoService,
    private tipoSolicitudService: TipoSolicitudService,
    private petroleraService: PetroleraService,
    private socioService: SocioService,
    private empresaService: EmpresaService,
    private contratoSocioService: ContratoSocioService,
    private notificationService: NotificationService,
    private errorHandler: ErrorHandlerService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.contratoForm = this.fb.group({
      tipoSolicitud: [TipoSolicitudContrato.NUEVO, Validators.required],
      esAutonomo: [true, Validators.required],
      socioId: [null, Validators.required],
      empresaId: [null],
      petroleraId: [null, Validators.required],
      tipoContratoId: [null],
      tipoSolicitudPetroleraId: [null],
      contratoId: [null],
      solicitadoPor: [''],
      observaciones: ['']
    });
  }

  ngOnInit(): void {
    this.cargarDatosIniciales();
    this.configurarValidacionesCondicionales();

    // Inicializar mostrarSubsecciones según el valor inicial (NUEVO)
    const tipoInicial = this.contratoForm.get('tipoSolicitud')?.value;
    if (tipoInicial === TipoSolicitudContrato.NUEVO) {
      this.mostrarSubsecciones = true;
    }
  }

  cargarDatosIniciales(): void {
    // Cargar tipos de contrato
    this.tipoContratoService.getActivos().subscribe({
      next: (tipos) => {
        this.tiposContrato = tipos;
      },
      error: (err) => {
        console.error('Error al cargar tipos de contrato', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });

    // Cargar petroleras
    this.petroleraService.getAll().subscribe({
      next: (petroleras) => {
        // Solo petroleras activas que operan con contratos (null = sin restricción)
        this.petroleras = petroleras.filter(p => p.activa && petroleraPermite(p.operaContratos));
        this.petrolerasFiltradas = this.petroleras; // Inicialmente mostrar todas
      },
      error: (err) => {
        console.error('Error al cargar petroleras', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });

    // Cargar socios
    this.socioService.getAll().subscribe({
      next: (socios) => {
        this.socios = socios;
      },
      error: (err) => {
        console.error('Error al cargar socios', err);
      }
    });

    // Cargar empresas
    this.empresaService.getAll().subscribe({
      next: (empresas) => {
        this.empresas = empresas;
      },
      error: (err) => {
        console.error('Error al cargar empresas', err);
      }
    });
  }

  configurarValidacionesCondicionales(): void {
    // Manejar cambios en tipo de solicitud
    this.contratoForm.get('tipoSolicitud')?.valueChanges.subscribe((tipo: TipoSolicitudContrato) => {
      this.onTipoSolicitudChange(tipo);
    });

    // Validar empresaId solo si no es autónomo
    this.contratoForm.get('esAutonomo')?.valueChanges.subscribe((esAutonomo: boolean) => {
      const empresaIdControl = this.contratoForm.get('empresaId');
      if (esAutonomo) {
        empresaIdControl?.clearValidators();
        empresaIdControl?.setValue(null);
      } else {
        empresaIdControl?.setValidators([Validators.required]);
      }
      empresaIdControl?.updateValueAndValidity();
    });

    // Filtrar empresas cuando cambie el socio seleccionado
    this.contratoForm.get('socioId')?.valueChanges.subscribe((socioId: string) => {
      this.onSocioChange(socioId);
    });

    // Cargar subsecciones cuando cambie la petrolera
    this.contratoForm.get('petroleraId')?.valueChanges.subscribe(() => {
      this.onPetroleraChange();
      this.cargarContratosActivosParaBaja();
    });
  }

  onSocioChange(socioId: string): void {
    // Limpiar la empresa seleccionada
    this.contratoForm.patchValue({ empresaId: null });

    // Si no hay socio seleccionado, limpiar la lista de empresas filtradas
    if (!socioId) {
      this.empresasFiltradas = [];
      return;
    }

    // Filtrar empresas por socio
    this.empresasFiltradas = this.empresas.filter(empresa => empresa.socioId === socioId);
  }

  onPetroleraChange(): void {
    const petroleraId = this.contratoForm.get('petroleraId')?.value;
    const tipoSolicitud = this.contratoForm.get('tipoSolicitud')?.value;

    // Solo cargar tipos de solicitud si es NUEVO y hay petrolera seleccionada
    if (petroleraId && tipoSolicitud === TipoSolicitudContrato.NUEVO) {
      this.cargarTiposSolicitud(petroleraId);
    } else {
      this.tiposSolicitud = [];
      this.contratoForm.patchValue({ tipoSolicitudPetroleraId: null });
    }
  }

  cargarTiposSolicitud(petroleraId: string): void {
    this.tipoSolicitudService.getByPetroleraId(petroleraId).subscribe({
      next: (tipos: TipoSolicitud[]) => {
        this.tiposSolicitud = tipos.filter((t: TipoSolicitud) => t.activa);
        const tipoSolicitudControl = this.contratoForm.get('tipoSolicitudPetroleraId');
        if (this.tiposSolicitud.length > 0) {
          // Si hay tipos de solicitud disponibles, hacerlo obligatorio
          tipoSolicitudControl?.setValidators([Validators.required]);
        } else {
          tipoSolicitudControl?.clearValidators();
          tipoSolicitudControl?.setValue(null);
        }
        tipoSolicitudControl?.updateValueAndValidity();
      },
      error: (err: any) => {
        console.error('Error al cargar tipos de solicitud', err);
        this.tiposSolicitud = [];
      }
    });
  }

  onTipoSolicitudChange(tipo: TipoSolicitudContrato): void {
    const contratoIdControl = this.contratoForm.get('contratoId');
    const tipoSolicitudPetroleraControl = this.contratoForm.get('tipoSolicitudPetroleraId');

    // Para BAJA y CAMBIO_CONDICIONES, contratoId es obligatorio
    if (tipo === TipoSolicitudContrato.BAJA || tipo === TipoSolicitudContrato.CAMBIO_CONDICIONES) {
      contratoIdControl?.setValidators([Validators.required]);
    } else {
      // Para NUEVO, contratoId no es necesario
      contratoIdControl?.clearValidators();
      contratoIdControl?.setValue(null);
    }

    contratoIdControl?.updateValueAndValidity();

    // Limpiar tipo de solicitud petrolera al cambiar tipo
    if (tipo !== TipoSolicitudContrato.NUEVO) {
      tipoSolicitudPetroleraControl?.clearValidators();
      tipoSolicitudPetroleraControl?.setValue(null);
      tipoSolicitudPetroleraControl?.updateValueAndValidity();
      this.tiposSolicitud = [];
    }

    // Para NUEVO, mostrar subsecciones (plantillas PDF)
    this.mostrarSubsecciones = (tipo === TipoSolicitudContrato.NUEVO);

    // Limpiar contratos activos y recargar si corresponde
    this.contratosActivos = [];

    // Filtrar petroleras según el tipo
    this.filtrarPetroleras();

    // Limpiar petrolera seleccionada al cambiar tipo
    this.contratoForm.patchValue({ petroleraId: null });

    this.cargarContratosActivosParaBaja();
  }

  cargarContratosActivosParaBaja(): void {
    const tipoSolicitud = this.contratoForm.get('tipoSolicitud')?.value;

    // Solo cargar para BAJA o CAMBIO_CONDICIONES
    if (tipoSolicitud !== TipoSolicitudContrato.BAJA &&
        tipoSolicitud !== TipoSolicitudContrato.CAMBIO_CONDICIONES) {
      return;
    }

    const socioId = this.contratoForm.get('socioId')?.value;
    const petroleraId = this.contratoForm.get('petroleraId')?.value;

    if (!socioId || !petroleraId) {
      this.contratosActivos = [];
      return;
    }

    this.contratoSocioService.getActivosBySocioAndPetrolera(socioId, petroleraId).subscribe({
      next: (contratos) => {
        this.contratosActivos = contratos;
        if (contratos.length === 0) {
          this.notificationService.error('No hay contratos activos para este socio y petrolera');
        }
      },
      error: (err) => {
        console.error('Error al cargar contratos activos', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
        this.contratosActivos = [];
      }
    });
  }

  onSocioSeleccionado(socio: Socio): void {
    this.selectedSocio = socio;
    this.contratoForm.patchValue({ socioId: socio.id });
    this.onSocioChange(String(socio.id));
    this.filtrarPetroleras();
    this.cargarContratosActivosParaBaja();
  }

  filtrarPetroleras(): void {
    const tipoSolicitud = this.contratoForm.get('tipoSolicitud')?.value;
    const socioId = this.contratoForm.get('socioId')?.value;

    // Si es NUEVO, mostrar todas las petroleras activas
    if (tipoSolicitud === TipoSolicitudContrato.NUEVO) {
      this.petrolerasFiltradas = this.petroleras;
      return;
    }

    // Si es BAJA o CAMBIO_CONDICIONES, filtrar por las que tienen contratos activos
    if ((tipoSolicitud === TipoSolicitudContrato.BAJA ||
         tipoSolicitud === TipoSolicitudContrato.CAMBIO_CONDICIONES) && socioId) {
      this.contratoSocioService.getPetrolerasActivasBySocio(Number(socioId)).subscribe({
        next: (petroleraIds) => {
          this.petrolerasFiltradas = this.petroleras.filter(p =>
            petroleraIds.includes(Number(p.id))
          );
        },
        error: (err) => {
          console.error('Error al cargar petroleras activas del socio', err);
          this.petrolerasFiltradas = [];
        }
      });
    } else {
      this.petrolerasFiltradas = this.petroleras;
    }
  }

  onSubmit(): void {
    if (this.contratoForm.invalid) {
      this.contratoForm.markAllAsTouched();
      this.notificationService.error('Por favor, complete todos los campos obligatorios');
      return;
    }

    if (!confirm('¿Está seguro de crear esta solicitud de contrato?')) {
      return;
    }

    this.crearContrato();
  }

  private crearContrato(): void {
    this.loading = true;
    const formValue = this.contratoForm.value;
    const dto: CrearSolicitudDTO = {
      ...formValue,
      tipoSolicitud: formValue.tipoSolicitud || TipoSolicitudContrato.NUEVO
    };

    this.solicitudService.crear(dto).subscribe({
      next: (solicitud) => {
        // Si hay un PDF editado, subirlo
        if (this.pdfEditado && solicitud.id) {
          this.subirPdfEditado(solicitud.id);
        } else {
          this.loading = false;
          this.notificationService.success('Contrato creado correctamente');
          this.router.navigate(['/contratos', solicitud.id]);
        }
      },
      error: (err) => {
        this.loading = false;
        console.error('Error al crear contrato', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
      }
    });
  }

  private subirPdfEditado(solicitudId: number): void {
    if (!this.pdfEditado) return;

    this.solicitudService.guardarPdfEditado(solicitudId, this.pdfEditado).subscribe({
      next: () => {
        this.loading = false;
        this.notificationService.success('Contrato creado y PDF editado guardado correctamente');
        this.pdfEditado = null;
        this.router.navigate(['/contratos', solicitudId]);
      },
      error: (err) => {
        this.loading = false;
        console.error('Error al subir PDF editado', err);
        this.notificationService.error(this.errorHandler.getMensaje(err));
        // Aún así navegamos al contrato
        this.router.navigate(['/contratos', solicitudId]);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/contratos']);
  }

  get f() {
    return this.contratoForm.controls;
  }
}
