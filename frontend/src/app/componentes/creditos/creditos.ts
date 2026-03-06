import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CreditoService } from '../../services/credito.service';
import { SocioService } from '../../services/socio.service';
import { EmpresaService } from '../../services/empresa.service';
import { PetroleraService } from '../../services/petrolera.service';
import { Credito, CrearCreditoDTO, TipoCredito, EstadoCredito } from '../../models/credito.model';
import { Socio } from '../../models/socio.model';
import { Empresa } from '../../models/empresa.model';
import { Petrolera } from '../../models/petrolera.model';
import { EmailLogs } from '../solicitudes-tarjetas/email-logs/email-logs';

@Component({
  selector: 'app-creditos',
  standalone: true,
  imports: [CommonModule, FormsModule, EmailLogs],
  templateUrl: './creditos.html',
  styleUrl: './creditos.css',
})
export class Creditos implements OnInit {
  creditos: Credito[] = [];
  socios: Socio[] = [];
  empresas: Empresa[] = [];
  petroleras: Petrolera[] = [];

  mostrarFormulario = false;
  modoEdicion = false;
  creditoSeleccionado?: Credito;

  nuevoCredito: CrearCreditoDTO = {
    socioId: 0,
    petroleraId: 0,
    tipoCredito: TipoCredito.SOLICITUD_CREDITO
  };

  filtroEstado: EstadoCredito | 'TODOS' = 'TODOS';

  TipoCredito = TipoCredito;
  EstadoCredito = EstadoCredito;

  tiposCredito = [
    { value: TipoCredito.SOLICITUD_CREDITO, label: 'Solicitud de Crédito' },
    { value: TipoCredito.AMPLIACION_CREDITO, label: 'Ampliación de Crédito' },
    { value: TipoCredito.DEVOLUCION_AVAL, label: 'Devolución de Aval' }
  ];

  // Autocomplete Socio
  busquedaSocio: string = '';
  sociosFiltrados: Socio[] = [];
  mostrarListaSocios: boolean = false;
  socioSeleccionado: Socio | null = null;

  // Modal de respuesta petrolera
  mostrarModalRespuesta = false;
  creditoRespondiendo?: Credito;
  aprobandoRespuesta: boolean = false;
  comentarioRespuesta: string = '';

  constructor(
    private creditoService: CreditoService,
    private socioService: SocioService,
    private empresaService: EmpresaService,
    private petroleraService: PetroleraService
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargarCreditos();
    this.cargarSocios();
    this.cargarPetroleras();
  }

  cargarCreditos(): void {
    this.creditoService.listarTodos().subscribe({
      next: (data) => {
        this.creditos = data;
      },
      error: (error) => {
        console.error('Error al cargar créditos:', error);
        alert('Error al cargar los créditos');
      }
    });
  }

  cargarSocios(): void {
    this.socioService.getAll().subscribe({
      next: (data) => {
        this.socios = data;
      },
      error: (error) => {
        console.error('Error al cargar socios:', error);
      }
    });
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

  onSocioChange(): void {
    if (this.nuevoCredito.socioId) {
      this.empresaService.getBySocioId(this.nuevoCredito.socioId.toString()).subscribe({
        next: (data) => {
          this.empresas = data;
        },
        error: (error) => {
          console.error('Error al cargar empresas:', error);
        }
      });
    }
  }

  abrirFormulario(): void {
    this.mostrarFormulario = true;
    this.modoEdicion = false;
    this.nuevoCredito = {
      socioId: 0,
      petroleraId: 0,
      tipoCredito: TipoCredito.SOLICITUD_CREDITO
    };
    this.limpiarBusquedaSocio();
  }

  cerrarFormulario(): void {
    this.mostrarFormulario = false;
    this.modoEdicion = false;
    this.creditoSeleccionado = undefined;
    this.limpiarBusquedaSocio();
  }

  // Métodos para autocomplete de socio
  buscarSocios(): void {
    const termino = this.busquedaSocio.toLowerCase().trim();

    if (!termino) {
      this.sociosFiltrados = [];
      this.mostrarListaSocios = false;
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
    this.nuevoCredito.socioId = Number(socio.id);
    this.mostrarListaSocios = false;
    this.onSocioChange();
  }

  limpiarBusquedaSocio(): void {
    this.busquedaSocio = '';
    this.socioSeleccionado = null;
    this.sociosFiltrados = [];
    this.mostrarListaSocios = false;
    this.nuevoCredito.socioId = 0;
    this.empresas = [];
  }

  guardarCredito(): void {
    if (!this.nuevoCredito.socioId || !this.nuevoCredito.petroleraId) {
      alert('Por favor complete todos los campos obligatorios');
      return;
    }

    this.creditoService.crear(this.nuevoCredito).subscribe({
      next: (credito) => {
        alert('Crédito creado exitosamente');
        this.cerrarFormulario();
        this.cargarCreditos();
      },
      error: (error) => {
        console.error('Error al crear crédito:', error);
        alert('Error al crear el crédito');
      }
    });
  }

  verDetalle(credito: Credito): void {
    this.creditoSeleccionado = credito;
  }

  enviarAPetrolera(credito: Credito): void {
    if (confirm('¿Está seguro de enviar este crédito a la petrolera?')) {
      this.creditoService.enviarAPetrolera(credito.id!).subscribe({
        next: () => {
          alert('Crédito enviado a la petrolera exitosamente');
          this.cargarCreditos();
        },
        error: (error) => {
          console.error('Error al enviar crédito:', error);
          alert('Error al enviar el crédito');
        }
      });
    }
  }

  abrirModalRespuesta(credito: Credito, aprobado: boolean): void {
    this.creditoRespondiendo = credito;
    this.aprobandoRespuesta = aprobado;
    this.comentarioRespuesta = '';
    this.mostrarModalRespuesta = true;
  }

  cerrarModalRespuesta(): void {
    this.mostrarModalRespuesta = false;
    this.creditoRespondiendo = undefined;
    this.comentarioRespuesta = '';
  }

  confirmarRespuesta(): void {
    if (!this.creditoRespondiendo) return;
    this.creditoService.responderPetrolera(
      this.creditoRespondiendo.id!,
      this.aprobandoRespuesta,
      this.comentarioRespuesta
    ).subscribe({
      next: () => {
        alert(`Crédito ${this.aprobandoRespuesta ? 'aprobado' : 'denegado'} exitosamente`);
        this.cerrarModalRespuesta();
        this.creditoSeleccionado = undefined;
        this.cargarCreditos();
      },
      error: (error) => {
        console.error('Error al responder crédito:', error);
        alert('Error al responder el crédito');
      }
    });
  }

  get creditosFiltrados(): Credito[] {
    if (this.filtroEstado === 'TODOS') {
      return this.creditos;
    }
    return this.creditos.filter(c => c.estado === this.filtroEstado);
  }

  getEstadoClass(estado: EstadoCredito): string {
    switch (estado) {
      case EstadoCredito.PENDIENTE:
        return 'badge-warning';
      case EstadoCredito.ENVIADO_PETROLERA:
        return 'badge-info';
      case EstadoCredito.APROBADO:
        return 'badge-success';
      case EstadoCredito.DENEGADO:
        return 'badge-danger';
      case EstadoCredito.COMPLETADO_APROBADO:
        return 'badge-success';
      case EstadoCredito.COMPLETADO_DENEGADO:
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }

  getEstadoTexto(estado: EstadoCredito): string {
    switch (estado) {
      case EstadoCredito.PENDIENTE: return 'Pendiente';
      case EstadoCredito.ENVIADO_PETROLERA: return 'Enviado a Petrolera';
      case EstadoCredito.APROBADO: return 'Aprobado';
      case EstadoCredito.DENEGADO: return 'Denegado';
      case EstadoCredito.COMPLETADO_APROBADO: return 'Completado (Aprobado)';
      case EstadoCredito.COMPLETADO_DENEGADO: return 'Completado (Denegado)';
      default: return estado;
    }
  }

  getTipoCreditoLabel(tipo: TipoCredito): string {
    const found = this.tiposCredito.find(t => t.value === tipo);
    return found ? found.label : tipo;
  }

  onTipoCreditoChange(): void {
    if (this.nuevoCredito.tipoCredito === TipoCredito.DEVOLUCION_AVAL) {
      this.nuevoCredito.monto = undefined;
    }
  }
}
