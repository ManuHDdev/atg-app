import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './services/auth.service';

@Component({ standalone: true, template: '<p>dummy</p>' })
class DummyPage {}

describe('App', () => {
  let fixture: ComponentFixture<App>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let router: Router;

  const query = (selector: string): HTMLElement | null =>
    (fixture.nativeElement as HTMLElement).querySelector(selector);

  const queryAll = (selector: string): HTMLElement[] =>
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll(selector));

  beforeEach(async () => {
    authServiceSpy = jasmine.createSpyObj('AuthService', [
      'isAdmin', 'isDeveloper', 'isAuthenticated', 'getUserFullName', 'logout'
    ]);
    authServiceSpy.isAdmin.and.returnValue(false);
    authServiceSpy.isDeveloper.and.returnValue(false);
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.getUserFullName.and.returnValue('Usuario de prueba');

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'inicio', component: DummyPage },
          { path: 'socios', component: DummyPage },
          { path: 'plantillas-email', component: DummyPage }
        ]),
        { provide: AuthService, useValue: authServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(App);
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the sidebar', () => {
    expect(query('.sidebar')).toBeTruthy();
  });

  describe('estructura del menú', () => {
    it('renderiza los grupos como encabezados reales', () => {
      const headings = queryAll('.sidebar-nav h2').map(h => h.textContent?.trim());
      expect(headings).toEqual(['Trámites', 'Datos', 'Configuración']);
    });

    it('asocia cada grupo con su encabezado mediante aria-labelledby', () => {
      queryAll('.sidebar-group').forEach(group => {
        const labelledBy = group.getAttribute('aria-labelledby');
        expect(labelledBy).toBeTruthy();
        expect(query('#' + labelledBy)).toBeTruthy();
      });
    });

    it('incluye el enlace de Inicio fuera de los grupos', () => {
      const inicio = query('.sidebar-nav > .sidebar-link');
      expect(inicio?.getAttribute('href')).toBe('/inicio');
      expect(inicio?.textContent?.trim()).toBe('Inicio');
    });

    it('hace alcanzables las pantallas que antes no tenían enlace', () => {
      const hrefs = queryAll('.sidebar-nav a').map(a => a.getAttribute('href'));
      expect(hrefs).toContain('/plantillas-email');
      expect(hrefs).toContain('/petroleras/tipos-solicitud');
      expect(hrefs).toContain('/plantillas-tarjetas');
    });

    it('mantiene el grupo de administración oculto sin rol admin ni developer', () => {
      expect(query('#grupo-administracion')).toBeNull();
      const hrefs = queryAll('.sidebar-nav a').map(a => a.getAttribute('href'));
      expect(hrefs).not.toContain('/usuarios');
      expect(hrefs).not.toContain('/incidencias');
    });

    it('muestra Gestión de Usuarios solo para admin e Incidencias para developer', () => {
      authServiceSpy.isAdmin.and.returnValue(false);
      authServiceSpy.isDeveloper.and.returnValue(true);
      fixture.detectChanges();

      let hrefs = queryAll('.sidebar-nav a').map(a => a.getAttribute('href'));
      expect(query('#grupo-administracion')?.textContent?.trim()).toBe('Administración');
      expect(hrefs).not.toContain('/usuarios');
      expect(hrefs).toContain('/incidencias');

      authServiceSpy.isAdmin.and.returnValue(true);
      fixture.detectChanges();

      hrefs = queryAll('.sidebar-nav a').map(a => a.getAttribute('href'));
      expect(hrefs).toContain('/usuarios');
      expect(hrefs).toContain('/incidencias');
    });
  });

  describe('cajón de navegación', () => {
    it('empieza cerrado y el botón apunta al menú', () => {
      const toggle = query('.topbar-toggle');
      expect(toggle?.getAttribute('aria-expanded')).toBe('false');
      expect(toggle?.getAttribute('aria-controls')).toBe('menu-principal');
      expect(query('#menu-principal')?.tagName).toBe('NAV');
      expect(query('.drawer-overlay')).toBeNull();
    });

    it('cambia aria-expanded al pulsar el botón', () => {
      const toggle = query('.topbar-toggle') as HTMLButtonElement;

      toggle.click();
      fixture.detectChanges();
      expect(toggle.getAttribute('aria-expanded')).toBe('true');
      expect(query('.drawer-overlay')).toBeTruthy();
      expect(query('.sidebar')?.classList).toContain('sidebar-open');

      toggle.click();
      fixture.detectChanges();
      expect(toggle.getAttribute('aria-expanded')).toBe('false');
      expect(query('.drawer-overlay')).toBeNull();
    });

    it('mueve el foco al menú al abrir y lo devuelve al botón al cerrar', fakeAsync(() => {
      const toggle = query('.topbar-toggle') as HTMLButtonElement;

      toggle.click();
      fixture.detectChanges();
      tick();
      expect(document.activeElement).toBe(query('#menu-principal'));

      // El botón está oculto (display:none) en el ancho de escritorio del runner,
      // así que se comprueba la llamada al foco en lugar de document.activeElement.
      const focusSpy = spyOn(toggle, 'focus').and.callThrough();
      toggle.click();
      fixture.detectChanges();
      expect(focusSpy).toHaveBeenCalled();
    }));

    it('se cierra al pulsar Escape', () => {
      fixture.componentInstance.openDrawer();
      fixture.detectChanges();
      expect(fixture.componentInstance.drawerOpen()).toBe(true);

      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
      fixture.detectChanges();

      expect(fixture.componentInstance.drawerOpen()).toBe(false);
      expect(query('.topbar-toggle')?.getAttribute('aria-expanded')).toBe('false');
    });

    it('se cierra al hacer clic en la superposición', () => {
      fixture.componentInstance.openDrawer();
      fixture.detectChanges();

      (query('.drawer-overlay') as HTMLElement).click();
      fixture.detectChanges();

      expect(fixture.componentInstance.drawerOpen()).toBe(false);
    });

    it('se cierra al navegar', async () => {
      fixture.componentInstance.openDrawer();
      fixture.detectChanges();

      await router.navigate(['/socios']);
      fixture.detectChanges();

      expect(fixture.componentInstance.drawerOpen()).toBe(false);
      expect(query('.topbar-toggle')?.getAttribute('aria-expanded')).toBe('false');
    });
  });

  describe('accesibilidad del armazón', () => {
    it('expone el enlace de salto como primer elemento enfocable hacia el contenido principal', () => {
      const skip = (fixture.nativeElement as HTMLElement).querySelector('a');
      expect(skip?.classList).toContain('skip-link');
      expect(skip?.getAttribute('href')).toBe('#contenido-principal');

      const main = query('main');
      expect(main?.id).toBe('contenido-principal');
      expect(main?.getAttribute('tabindex')).toBe('-1');
    });

    it('lleva el foco al contenido principal al activar el enlace de salto', () => {
      (query('.skip-link') as HTMLAnchorElement).click();
      fixture.detectChanges();
      expect(document.activeElement).toBe(query('main'));
    });

    it('da nombre accesible a los landmarks y al logotipo', () => {
      expect(query('nav')?.getAttribute('aria-label')).toBe('Navegación principal');
      expect(query('aside')?.getAttribute('aria-label')).toBe('Panel lateral');
      expect(query('.sidebar-header a')?.getAttribute('aria-label')).toBeTruthy();
    });

    it('marca la página activa con aria-current', async () => {
      await router.navigate(['/socios']);
      fixture.detectChanges();

      const activos = queryAll('.sidebar-nav a[aria-current="page"]').map(a => a.getAttribute('href'));
      expect(activos).toEqual(['/socios']);
    });

    it('oculta los iconos decorativos a los lectores de pantalla', () => {
      const iconos = queryAll('.sidebar i, .topbar i');
      expect(iconos.length).toBeGreaterThan(0);
      iconos.forEach(icono => expect(icono.getAttribute('aria-hidden')).toBe('true'));
    });

    it('etiqueta los botones que solo tienen icono', () => {
      expect(query('.topbar-toggle')?.getAttribute('aria-label')).toBe('Abrir menú de navegación');
      expect(query('.sidebar-logout')?.getAttribute('aria-label')).toBe('Cerrar sesión');
    });
  });
});
