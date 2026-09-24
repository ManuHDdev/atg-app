import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { SolicitudTarjetaService } from '../../services/solicitud-tarjeta.service';
import { SocioService } from '../../services/socio.service';
import { PetroleraService } from '../../services/petrolera.service';
import { TarjetaService } from '../../services/tarjeta.service';
import { Socio } from '../../models/socio.model';
import { Petrolera, petroleraPermite } from '../../models/petrolera.model';
import { Tarjeta } from '../../models/tarjeta.model';
import { SocioAutocomplete } from '../shared/socio-autocomplete/socio-autocomplete';
import { ErrorHandlerService } from '../../services/error-handler.service';
import { MOTIVOS_DUPLICADO } from '../../models/solicitud-tarjeta.model';

@Component({
  selector: 'app-solicitud-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SocioAutocomplete],
  templateUrl: './solicitud-form.html',
  styleUrl: './solicitud-form.css'
})
export class SolicitudForm implements OnInit {
  formulario!: FormGroup;
  tipoSolicitud: string = '';
  tipo: string = ''; // Alias para usar en el template

  socios: Socio[] = [];
  petroleras: Petrolera[] = [];
  tarjetasDelSocio: Tarjeta[] = [];
  selectedSocio: Socio | null = null;

  loading: boolean = false;
  error: string | null = null;
  success: boolean = false;
  intentoGuardar: boolean = false;

  // Los dos únicos motivos reales de un duplicado (impreso DOCUMENTO 7)
  readonly motivosDuplicado = MOTIVOS_DUPLICADO;

  constructor(
    private fb: FormBuilder,
    private solicitudService: SolicitudTarjetaService,
    private socioService: SocioService,
    private petroleraService: PetroleraService,
    private tarjetaService: TarjetaService,
    private router: Router,
    private route: ActivatedRoute,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.tipoSolicitud = this.route.snapshot.paramMap.get('tipo')?.toUpperCase() || 'ALTA';
    this.tipo = this.tipoSolicitud; // Sincronizar con alias
    this.inicializarFormulario();
    this.cargarDatos();
  }

  inicializarFormulario(): void {
    const formConfig: any = {
      socioId: ['', Validators.required],
      petroleraId: ['', Validators.required],
      matricula: ['', [Validators.required, Validators.maxLength(20)]],  // Obligatorio
      numeroContrato: [''],  // Opcional
      observaciones: ['', Validators.maxLength(500)]
    };

    // Campos específicos por tipo de solicitud
    switch (this.tipoSolicitud) {
      case 'LLEGADA':
        formConfig.fechaLlegadaEstimada = ['', Validators.required];
        break;

      case 'ALTA':
        formConfig.solicitadoPor = ['', Validators.required];  // Persona de oficina
        break;

      case 'BAJA':
        formConfig.tarjetaId = ['', Validators.required];  // Tarjeta a dar de baja
        break;

      case 'DUPLICADO':
        formConfig.tarjetaId = ['', Validators.required];  // Tarjeta a duplicar
        formConfig.motivoDuplicado = ['', Validators.required];  // Deterioro o extravío
        break;
    }

    this.formulario = this.fb.group(formConfig);

    // En BAJA y DUPLICADO, escuchar cambios en tarjetaId para rellenar matrícula automáticamente
    if (this.tipoSolicitud === 'BAJA' || this.tipoSolicitud === 'DUPLICADO') {
      this.formulario.get('tarjetaId')?.valueChanges.subscribe(tarjetaId => {
        this.onTarjetaSelected(tarjetaId);
      });
    }
  }

  cargarDatos(): void {
    this.loading = true;

    this.socioService.getAll().subscribe({
      next: (socios) => {
        this.socios = socios.filter(s => s.activo);
      },
      error: (err) => console.error('Error al cargar socios:', err)
    });

    this.petroleraService.getAll().subscribe({
      next: (petroleras) => {
        // Solo petroleras activas que operan con tarjetas (null = sin restricción)
        this.petroleras = petroleras.filter(p => p.activa && petroleraPermite(p.operaTarjetas));
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar petroleras:', err);
        this.loading = false;
      }
    });
  }

  /** Un campo se marca en rojo cuando ya se ha intentado guardar o el usuario lo ha tocado. */
  campoInvalido(nombre: string): boolean {
    const control = this.formulario.get(nombre);
    if (!control) return false;
    return control.invalid && (this.intentoGuardar || control.touched);
  }

  /** Lleva el foco al primer control inválido, en el orden en que se ven en pantalla. */
  private enfocarPrimerCampoInvalido(): void {
    const primero = Object.keys(this.formulario.controls)
      .find(nombre => this.formulario.get(nombre)?.invalid);
    if (primero) {
      document.getElementById(primero)?.focus();
    }
  }

  onSubmit(): void {
    this.intentoGuardar = true;

    if (this.formulario.invalid) {
      Object.keys(this.formulario.controls).forEach(key => {
        this.formulario.get(key)?.markAsTouched();
      });
      // Marcar en rojo no basta si el campo que falla ha quedado fuera de la vista:
      // se lleva el foco al primero para que el operador lo vea y lo oiga.
      this.enfocarPrimerCampoInvalido();
      return;
    }

    this.loading = true;
    this.error = null;

    // Limpiar valores vacíos (convertir "" a null)
    const formValue = { ...this.formulario.value };
    Object.keys(formValue).forEach(key => {
      if (formValue[key] === '' || formValue[key] === undefined) {
        formValue[key] = null;
      }
    });

    const solicitud = {
      ...formValue,
      tipo: this.tipoSolicitud
    };

    this.solicitudService.create(solicitud).subscribe({
      next: () => {
        this.success = true;
        setTimeout(() => {
          this.router.navigate(['/solicitudes-tarjetas/dashboard']);
        }, 1500);
      },
      error: (err) => {
        console.error('Error al crear solicitud:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/solicitudes-tarjetas/dashboard']);
  }

  onSocioSelected(socio: Socio): void {
    this.selectedSocio = socio;

    // Si es una solicitud de BAJA o DUPLICADO, cargar las tarjetas del socio
    if (this.tipoSolicitud === 'BAJA' || this.tipoSolicitud === 'DUPLICADO') {
      this.cargarTarjetasDelSocio();
    }
  }

  onPetroleraSelected(): void {
    // Si es una solicitud de BAJA o DUPLICADO y ya hay un socio seleccionado, recargar tarjetas
    if ((this.tipoSolicitud === 'BAJA' || this.tipoSolicitud === 'DUPLICADO') && this.selectedSocio) {
      this.cargarTarjetasDelSocio();
    }
  }

  cargarTarjetasDelSocio(): void {
    const petroleraId = this.formulario.get('petroleraId')?.value;

    if (!this.selectedSocio || !petroleraId) {
      this.tarjetasDelSocio = [];
      return;
    }

    this.loading = true;
    this.error = null;

    this.tarjetaService.getAll().subscribe({
      next: (tarjetas) => {
        // Convertir a string para comparación (petrolera.id es number, tarjeta.petroleraId es string)
        const socioIdStr = String(this.selectedSocio!.id);
        const petroleraIdStr = String(petroleraId);

        this.tarjetasDelSocio = tarjetas.filter(t =>
          String(t.socioId) === socioIdStr &&
          String(t.petroleraId) === petroleraIdStr &&
          t.activa
        );
        this.loading = false;

        if (this.tarjetasDelSocio.length === 0) {
          this.error = 'Este socio no tiene tarjetas activas con la petrolera seleccionada';
        }
      },
      error: (err) => {
        console.error('Error al cargar tarjetas:', err);
        this.error = this.errorHandler.getMensaje(err);
        this.loading = false;
      }
    });
  }

  onTarjetaSelected(tarjetaId: string): void {
    if (!tarjetaId) {
      // Si se deselecciona, limpiar matrícula
      this.formulario.patchValue({ matricula: '' });
      return;
    }

    // Buscar la tarjeta seleccionada y rellenar la matrícula
    const tarjetaSeleccionada = this.tarjetasDelSocio.find(t => String(t.id) === String(tarjetaId));
    if (tarjetaSeleccionada) {
      this.formulario.patchValue({ matricula: tarjetaSeleccionada.matricula });
    }
  }

  getTituloFormulario(): string {
    switch (this.tipoSolicitud) {
      case 'LLEGADA': return 'Registro de Llegada de Tarjeta';
      case 'ALTA': return 'Solicitud de Alta de Tarjeta';
      case 'BAJA': return 'Solicitud de Baja de Tarjeta';
      case 'DUPLICADO': return 'Solicitud de Duplicado de Tarjeta';
      default: return 'Nueva Solicitud';
    }
  }

  getDescripcionFormulario(): string {
    switch (this.tipoSolicitud) {
      case 'LLEGADA':
        return 'Registra la llegada de tarjetas con su fecha. Se enviará automáticamente un correo al socio según su provincia (recogida en Madrid, envío postal fuera). Después solo queda registrar la entrega.';
      case 'ALTA':
        return 'Solicita el alta de una nueva tarjeta. Se genera el impreso de la petrolera para que lo firme el socio: cuando lo devuelva firmado, se sube aquí y se remite a la petrolera.';
      case 'BAJA':
        return 'Solicita la baja de una tarjeta existente. Se genera el impreso para que lo firme el socio y, una vez firmado, se remite a la petrolera.';
      case 'DUPLICADO':
        return 'Solicita un duplicado de tarjeta indicando el motivo. Se genera el impreso para que lo firme el socio y se remite a la petrolera; después habrá que registrar la llegada y la entrega, como en un alta.';
      default: return '';
    }
  }
}
