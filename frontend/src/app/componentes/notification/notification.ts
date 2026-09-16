import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificationService, Notification } from '../../services/notification.service';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification.html',
  styleUrl: './notification.css'
})
export class NotificationComponent implements OnInit, OnDestroy {
  notifications: (Notification & { id: number })[] = [];
  private subscription?: Subscription;
  private nextId = 0;

  constructor(private notificationService: NotificationService) {}

  ngOnInit() {
    this.subscription = this.notificationService.notification$.subscribe(
      (notification) => {
        const id = this.nextId++;
        const notificationWithId = { ...notification, id };
        this.notifications.push(notificationWithId);

        // Auto-remover después de la duración especificada
        setTimeout(() => {
          this.removeNotification(id);
        }, notification.duration || 3000);
      }
    );
  }

  ngOnDestroy() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  removeNotification(id: number) {
    this.notifications = this.notifications.filter(n => n.id !== id);
  }

  /** Prefijo textual para que un lector de pantalla distinga el tipo de aviso. */
  getTypeLabel(type: string): string {
    switch (type) {
      case 'success': return 'Correcto';
      case 'error': return 'Error';
      case 'warning': return 'Aviso';
      case 'info': return 'Información';
      default: return 'Información';
    }
  }

  getIconClass(type: string): string {
    switch (type) {
      case 'success': return 'bi-check-circle-fill';
      case 'error': return 'bi-x-circle-fill';
      case 'warning': return 'bi-exclamation-triangle-fill';
      case 'info': return 'bi-info-circle-fill';
      default: return 'bi-info-circle-fill';
    }
  }
}
