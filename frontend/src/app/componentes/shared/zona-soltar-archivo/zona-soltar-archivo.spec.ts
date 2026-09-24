import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ZonaSoltarArchivo } from './zona-soltar-archivo';

describe('ZonaSoltarArchivo', () => {
  let component: ZonaSoltarArchivo;
  let fixture: ComponentFixture<ZonaSoltarArchivo>;

  function pdf(nombre = 'impreso.pdf', bytes = 1024): File {
    const contenido = new Uint8Array(bytes);
    return new File([contenido], nombre, { type: 'application/pdf' });
  }

  /** DataTransfer no es instanciable en todos los navegadores: se simula lo justo. */
  function eventoArrastre(
    tipo: string,
    datos?: { archivos?: File[]; elementos?: any[] }
  ): DragEvent {
    const evento = new Event(tipo, { bubbles: true, cancelable: true }) as any;
    evento.dataTransfer = {
      files: datos?.archivos ?? [],
      items: datos?.elementos ?? [],
      dropEffect: 'none'
    };
    return evento as DragEvent;
  }

  function zona(): HTMLElement {
    return fixture.nativeElement.querySelector('.zona') as HTMLElement;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZonaSoltarArchivo]
    }).compileComponents();

    fixture = TestBed.createComponent(ZonaSoltarArchivo);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('archivo soltado', () => {
    it('emite el archivo cuando es válido y lo muestra como seleccionado', () => {
      const emitido: File[] = [];
      component.archivoSeleccionado.subscribe(archivo => emitido.push(archivo));

      const archivo = pdf();
      component.alSoltar(eventoArrastre('drop', { archivos: [archivo] }));
      fixture.detectChanges();

      expect(emitido).toEqual([archivo]);
      expect(component.archivo).toBe(archivo);
      expect(component.mensajeRechazo).toBe('');
      expect(fixture.nativeElement.textContent).toContain('impreso.pdf');
    });

    it('no traga el evento: el padre decide qué hacer con el archivo válido', () => {
      // El único efecto del componente es emitirlo; no sube nada por su cuenta.
      let recibido: File | null = null;
      component.archivoSeleccionado.subscribe(archivo => (recibido = archivo));

      component.alSoltar(eventoArrastre('drop', { archivos: [pdf()] }));

      expect(recibido).not.toBeNull();
    });

    it('rechaza un archivo que no sea del tipo aceptado con un mensaje en español', () => {
      const rechazos: string[] = [];
      component.archivoRechazado.subscribe(mensaje => rechazos.push(mensaje));

      const imagen = new File([''], 'escaneo.png', { type: 'image/png' });
      component.alSoltar(eventoArrastre('drop', { archivos: [imagen] }));
      fixture.detectChanges();

      expect(component.archivo).toBeNull();
      expect(rechazos).toEqual(['Solo se admiten archivos PDF.']);
      expect(component.mensajeRechazo).toBe('Solo se admiten archivos PDF.');
    });

    it('rechaza un archivo que supera el tamaño máximo', () => {
      component.tamanoMaximoMb = 1;
      const rechazos: string[] = [];
      component.archivoRechazado.subscribe(mensaje => rechazos.push(mensaje));

      component.alSoltar(eventoArrastre('drop', { archivos: [pdf('grande.pdf', 2 * 1024 * 1024)] }));

      expect(component.archivo).toBeNull();
      expect(rechazos).toEqual(['El archivo supera el tamaño máximo permitido (1 MB).']);
    });

    it('rechaza una carpeta soltada', () => {
      const carpeta = { webkitGetAsEntry: () => ({ isDirectory: true }) };

      component.alSoltar(eventoArrastre('drop', { archivos: [pdf()], elementos: [carpeta] }));

      expect(component.archivo).toBeNull();
      expect(component.mensajeRechazo).toBe('No se pueden subir carpetas, solo archivos.');
    });

    it('con varios archivos soltados se queda solo con el primero', () => {
      const primero = pdf('primero.pdf');
      const segundo = pdf('segundo.pdf');
      const emitidos: File[] = [];
      component.archivoSeleccionado.subscribe(archivo => emitidos.push(archivo));

      component.alSoltar(eventoArrastre('drop', { archivos: [primero, segundo] }));

      expect(emitidos).toEqual([primero]);
    });

    it('evita que el navegador abra el archivo soltado en otra pestaña', () => {
      const encima = eventoArrastre('dragover');
      spyOn(encima, 'preventDefault');
      const soltar = eventoArrastre('drop', { archivos: [pdf()] });
      spyOn(soltar, 'preventDefault');

      component.alArrastrarEncima(encima);
      component.alSoltar(soltar);

      expect(encima.preventDefault).toHaveBeenCalled();
      expect(soltar.preventDefault).toHaveBeenCalled();
    });
  });

  describe('resalte de arrastre', () => {
    it('se enciende al entrar el arrastre en la zona', () => {
      component.alEntrarArrastre(eventoArrastre('dragenter'));
      fixture.detectChanges();

      expect(component.arrastrando).toBeTrue();
      expect(zona().classList).toContain('arrastrando');
    });

    it('sobrevive al dragleave que dispara un hijo de la zona', () => {
      // Entrar en la zona y después en un hijo: dos dragenter, un solo dragleave.
      component.alEntrarArrastre(eventoArrastre('dragenter'));
      component.alEntrarArrastre(eventoArrastre('dragenter'));
      component.alSalirArrastre(eventoArrastre('dragleave'));
      fixture.detectChanges();

      expect(component.arrastrando).toBeTrue();
      expect(zona().classList).toContain('arrastrando');

      component.alSalirArrastre(eventoArrastre('dragleave'));
      fixture.detectChanges();

      expect(component.arrastrando).toBeFalse();
      expect(zona().classList).not.toContain('arrastrando');
    });

    it('se apaga al soltar el archivo', () => {
      component.alEntrarArrastre(eventoArrastre('dragenter'));
      component.alSoltar(eventoArrastre('drop', { archivos: [pdf()] }));

      expect(component.arrastrando).toBeFalse();
    });
  });

  describe('selector de archivos', () => {
    it('al pulsar la zona sigue abriendo el selector del sistema', () => {
      const abrir = spyOn(component.entradaArchivo!.nativeElement, 'click');

      zona().click();

      expect(abrir).toHaveBeenCalled();
    });

    it('se activa con Enter y con Espacio desde el teclado', () => {
      const abrir = spyOn(component.entradaArchivo!.nativeElement, 'click');

      zona().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
      zona().dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }));

      expect(abrir).toHaveBeenCalledTimes(2);
    });

    it('ignora otras teclas', () => {
      const abrir = spyOn(component.entradaArchivo!.nativeElement, 'click');

      zona().dispatchEvent(new KeyboardEvent('keydown', { key: 'a', bubbles: true }));

      expect(abrir).not.toHaveBeenCalled();
    });

    it('valida igual el archivo elegido por el selector', () => {
      const emitidos: File[] = [];
      component.archivoSeleccionado.subscribe(archivo => emitidos.push(archivo));

      const entrada = document.createElement('input');
      const archivo = pdf();
      Object.defineProperty(entrada, 'files', { value: [archivo] });

      component.alCambiarEntrada({ target: entrada } as unknown as Event);

      expect(emitidos).toEqual([archivo]);
    });
  });

  describe('accesibilidad', () => {
    it('la zona es alcanzable por teclado y tiene nombre accesible', () => {
      component.nombreAccesible = 'Subir impreso firmado';
      fixture.detectChanges();

      expect(zona().getAttribute('tabindex')).toBe('0');
      expect(zona().getAttribute('role')).toBe('button');
      expect(zona().getAttribute('aria-label')).toBe('Subir impreso firmado');
    });

    it('describe los formatos admitidos con aria-describedby', () => {
      const idAyuda = zona().getAttribute('aria-describedby');
      const ayuda = fixture.nativeElement.querySelector(`#${idAyuda}`);

      expect(ayuda.textContent).toContain('Solo se admiten archivos PDF');
      expect(ayuda.textContent).toContain('50 MB');
    });

    it('anuncia el rechazo en una región aria-live', () => {
      component.alSoltar(eventoArrastre('drop', { archivos: [new File([''], 'x.txt', { type: 'text/plain' })] }));
      fixture.detectChanges();

      const region = fixture.nativeElement.querySelector('.zona-rechazo');
      expect(region.getAttribute('aria-live')).toBe('polite');
      expect(region.textContent).toContain('Solo se admiten archivos PDF.');
    });

    it('oculta los iconos a los lectores de pantalla', () => {
      const iconos = fixture.nativeElement.querySelectorAll('i.zona-icono');
      expect(iconos.length).toBeGreaterThan(0);
      iconos.forEach((icono: Element) => expect(icono.getAttribute('aria-hidden')).toBe('true'));
    });
  });

  describe('quitar la selección', () => {
    it('limpia el archivo y avisa al padre', () => {
      let limpiada = false;
      component.seleccionLimpiada.subscribe(() => (limpiada = true));

      component.alSoltar(eventoArrastre('drop', { archivos: [pdf()] }));
      component.quitarSeleccion();
      fixture.detectChanges();

      expect(component.archivo).toBeNull();
      expect(limpiada).toBeTrue();
      expect(fixture.nativeElement.textContent).not.toContain('impreso.pdf');
    });
  });

  describe('deshabilitada', () => {
    it('no acepta archivos soltados ni abre el selector', () => {
      component.deshabilitado = true;
      fixture.detectChanges();
      const abrir = spyOn(component.entradaArchivo!.nativeElement, 'click');

      component.alSoltar(eventoArrastre('drop', { archivos: [pdf()] }));
      component.abrirSelector();

      expect(component.archivo).toBeNull();
      expect(abrir).not.toHaveBeenCalled();
      expect(zona().getAttribute('tabindex')).toBe('-1');
    });
  });
});
