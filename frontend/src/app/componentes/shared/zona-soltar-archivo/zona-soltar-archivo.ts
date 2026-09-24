import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';

/** Contador global para generar ids únicos de `aria-describedby` entre instancias. */
let instancias = 0;

/**
 * Zona de subida reutilizable: acepta un archivo soltado encima y, además, sigue
 * abriendo el selector del sistema al pulsarla con el ratón o con Enter/Espacio.
 * Arrastrar y soltar es un añadido, nunca la única forma de subir.
 *
 * El componente valida tipo y tamaño y avisa del rechazo; la decisión de qué
 * hacer con un archivo válido es siempre del componente padre.
 */
@Component({
  selector: 'app-zona-soltar-archivo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './zona-soltar-archivo.html',
  styleUrl: './zona-soltar-archivo.css'
})
export class ZonaSoltarArchivo {
  /** Texto visible dentro de la zona cuando no hay archivo elegido. */
  @Input() etiqueta = 'Arrastre el archivo aquí o pulse para elegirlo';
  /** Nombre accesible de la zona (lo que anuncia el lector de pantalla). */
  @Input() nombreAccesible = 'Seleccionar archivo';
  /** Tipos aceptados: MIME, extensión o lista separada por comas. Vacío = todos. */
  @Input() tipoAceptado = 'application/pdf';
  /** Cómo se nombra el formato en los mensajes en español. */
  @Input() formatoLegible = 'PDF';
  /** Tamaño máximo admitido, en megabytes. */
  @Input() tamanoMaximoMb = 50;
  /** Archivo actualmente elegido. El padre lo pone a null para limpiar la zona. */
  @Input() archivo: File | null = null;
  @Input() deshabilitado = false;
  /** Variante de una sola línea, para barras de acciones donde no cabe la zona completa. */
  @Input() compacto = false;

  /** Solo se emite con un archivo que ha pasado todas las validaciones. */
  @Output() archivoSeleccionado = new EventEmitter<File>();
  /** Mensaje en español explicando por qué se ha rechazado el archivo. */
  @Output() archivoRechazado = new EventEmitter<string>();
  /** El usuario ha quitado el archivo elegido. */
  @Output() seleccionLimpiada = new EventEmitter<void>();

  @ViewChild('entradaArchivo') entradaArchivo?: ElementRef<HTMLInputElement>;

  readonly idAyuda = `zona-soltar-ayuda-${++instancias}`;

  /** Hay un arrastre encima de la zona: dispara el resalte visual. */
  arrastrando = false;
  mensajeRechazo = '';

  /**
   * `dragleave` también salta al pasar de la zona a un hijo suyo. Contando entradas
   * y salidas el resalte no parpadea mientras el puntero recorre el interior.
   */
  private profundidadArrastre = 0;

  get textoAyuda(): string {
    return `Solo se admiten archivos ${this.formatoLegible}. Tamaño máximo ${this.tamanoMaximoMb} MB.`;
  }

  get tamanoLegible(): string {
    if (!this.archivo) return '';
    const kb = this.archivo.size / 1024;
    return kb < 1024 ? `${kb.toFixed(1)} KB` : `${(kb / 1024).toFixed(2)} MB`;
  }

  // ---------- apertura del selector ----------

  abrirSelector(): void {
    if (this.deshabilitado) return;
    this.entradaArchivo?.nativeElement.click();
  }

  /** La zona es un `role="button"`: Enter y Espacio deben activarla como a un botón. */
  alPulsarTecla(evento: KeyboardEvent): void {
    if (evento.key !== 'Enter' && evento.key !== ' ' && evento.key !== 'Spacebar') return;
    evento.preventDefault();
    this.abrirSelector();
  }

  alCambiarEntrada(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const elegido = entrada.files?.[0] ?? null;
    // Se limpia siempre para que volver a elegir el mismo archivo dispare `change`.
    entrada.value = '';
    if (elegido) {
      this.validar(elegido);
    }
  }

  // ---------- arrastrar y soltar ----------

  alEntrarArrastre(evento: DragEvent): void {
    evento.preventDefault();
    evento.stopPropagation();
    if (this.deshabilitado) return;
    this.profundidadArrastre++;
    this.arrastrando = true;
  }

  /** Sin `preventDefault` el navegador no admite el drop y acaba abriendo el PDF en otra pestaña. */
  alArrastrarEncima(evento: DragEvent): void {
    evento.preventDefault();
    evento.stopPropagation();
    if (this.deshabilitado) return;
    if (evento.dataTransfer) {
      evento.dataTransfer.dropEffect = 'copy';
    }
    this.arrastrando = true;
  }

  alSalirArrastre(evento: DragEvent): void {
    evento.preventDefault();
    evento.stopPropagation();
    this.profundidadArrastre = Math.max(0, this.profundidadArrastre - 1);
    if (this.profundidadArrastre === 0) {
      this.arrastrando = false;
    }
  }

  alSoltar(evento: DragEvent): void {
    evento.preventDefault();
    evento.stopPropagation();
    this.profundidadArrastre = 0;
    this.arrastrando = false;
    if (this.deshabilitado) return;

    const transferencia = evento.dataTransfer;

    if (this.contieneCarpeta(transferencia)) {
      this.rechazar('No se pueden subir carpetas, solo archivos.');
      return;
    }

    const archivos = transferencia?.files;
    if (!archivos || archivos.length === 0) {
      this.rechazar('No se ha recibido ningún archivo.');
      return;
    }

    // Soltar varios archivos se queda con el primero, nunca sube el resto en silencio.
    this.validar(archivos[0]);
  }

  // ---------- selección ----------

  quitarSeleccion(): void {
    if (this.deshabilitado) return;
    this.archivo = null;
    this.mensajeRechazo = '';
    if (this.entradaArchivo?.nativeElement) {
      this.entradaArchivo.nativeElement.value = '';
    }
    this.seleccionLimpiada.emit();
  }

  private validar(candidato: File): void {
    if (!this.esTipoAceptado(candidato)) {
      this.rechazar(`Solo se admiten archivos ${this.formatoLegible}.`);
      return;
    }

    if (candidato.size > this.tamanoMaximoMb * 1024 * 1024) {
      this.rechazar(`El archivo supera el tamaño máximo permitido (${this.tamanoMaximoMb} MB).`);
      return;
    }

    this.mensajeRechazo = '';
    this.archivo = candidato;
    this.archivoSeleccionado.emit(candidato);
  }

  private esTipoAceptado(candidato: File): boolean {
    const patrones = this.tipoAceptado
      .split(',')
      .map(patron => patron.trim().toLowerCase())
      .filter(patron => patron.length > 0);

    if (patrones.length === 0) return true;

    const tipo = (candidato.type || '').toLowerCase();
    const nombre = candidato.name.toLowerCase();

    return patrones.some(patron => {
      if (patron.startsWith('.')) return nombre.endsWith(patron);
      if (patron.endsWith('/*')) return tipo.startsWith(patron.slice(0, -1));
      return tipo === patron;
    });
  }

  /**
   * Una carpeta soltada llega como entrada de directorio. Donde el navegador no
   * expone `webkitGetAsEntry` cae igualmente en la validación de tipo, porque una
   * carpeta no tiene MIME ni extensión admitida.
   */
  private contieneCarpeta(transferencia: DataTransfer | null): boolean {
    const elementos = transferencia?.items;
    if (!elementos || elementos.length === 0) return false;

    const obtenerEntrada = (elementos[0] as any)?.webkitGetAsEntry;
    if (typeof obtenerEntrada !== 'function') return false;

    return obtenerEntrada.call(elementos[0])?.isDirectory === true;
  }

  private rechazar(mensaje: string): void {
    this.mensajeRechazo = mensaje;
    this.archivoRechazado.emit(mensaje);
  }
}
