import { Component, signal } from '@angular/core';
import { RouterOutlet, Router, RouterLinkWithHref } from '@angular/router';
import { NotificationComponent } from './componentes/notification/notification';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NotificationComponent, RouterLinkWithHref],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');

  constructor(
    private router: Router,
    public auth: AuthService
  ) {}

  navegarA(ruta: string): void {
    this.router.navigate([`/${ruta}`]);
  }
}
