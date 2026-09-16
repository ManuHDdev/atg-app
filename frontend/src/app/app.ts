import { Component, ElementRef, HostListener, OnDestroy, signal, viewChild } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { NotificationComponent } from './componentes/notification/notification';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnDestroy {
  protected readonly title = signal('frontend');

  /** Estado del menú lateral en pantallas pequeñas (cajón deslizante). */
  readonly drawerOpen = signal(false);

  private readonly drawerToggle = viewChild<ElementRef<HTMLButtonElement>>('drawerToggle');
  private readonly drawerNav = viewChild<ElementRef<HTMLElement>>('drawerNav');
  private readonly navigationSub: Subscription;

  constructor(public auth: AuthService, private router: Router) {
    // Al navegar, el cajón se cierra para no tapar el contenido en móvil.
    this.navigationSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.closeDrawer());
  }

  ngOnDestroy(): void {
    this.navigationSub.unsubscribe();
  }

  toggleDrawer(): void {
    if (this.drawerOpen()) {
      this.closeDrawer();
    } else {
      this.openDrawer();
    }
  }

  openDrawer(): void {
    if (this.drawerOpen()) {
      return;
    }
    this.drawerOpen.set(true);
    // El foco entra en el cajón una vez aplicados los estilos de apertura.
    setTimeout(() => this.drawerNav()?.nativeElement.focus());
  }

  closeDrawer(): void {
    if (!this.drawerOpen()) {
      return;
    }
    this.drawerOpen.set(false);
    // El foco vuelve al botón que abrió el cajón.
    this.drawerToggle()?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closeDrawer();
  }

  /** Lleva el foco al contenido principal sin alterar la ruta activa. */
  skipToContent(event: Event): void {
    event.preventDefault();
    const main = document.getElementById('contenido-principal');
    main?.focus();
  }
}
