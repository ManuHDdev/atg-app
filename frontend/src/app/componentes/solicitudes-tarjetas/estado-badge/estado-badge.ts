import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'TARJETA_LLEGADA' | 'ENTREGADA' | 'COMPLETADA';

@Component({
  selector: 'app-estado-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './estado-badge.html',
  styleUrl: './estado-badge.css'
})
export class EstadoBadge {
  @Input() estado!: EstadoSolicitud;
  @Input() size: 'small' | 'medium' | 'large' = 'medium';

  get estadoClass(): string {
    return `badge-${this.estado.toLowerCase().replace('_', '-')}`;
  }

  get estadoLabel(): string {
    const labels: Record<EstadoSolicitud, string> = {
      'PENDIENTE': 'Pendiente',
      'APROBADA': 'Aprobada',
      'RECHAZADA': 'Rechazada',
      'TARJETA_LLEGADA': 'Tarjeta Llegada',
      'ENTREGADA': 'Entregada',
      'COMPLETADA': 'Completada'
    };
    return labels[this.estado] || this.estado;
  }

  get estadoIcon(): string {
    const icons: Record<EstadoSolicitud, string> = {
      'PENDIENTE': 'bi-hourglass-split',
      'APROBADA': 'bi-check-lg',
      'RECHAZADA': 'bi-x-lg',
      'TARJETA_LLEGADA': 'bi-box-seam',
      'ENTREGADA': 'bi-check2-all',
      'COMPLETADA': 'bi-patch-check-fill'
    };
    return icons[this.estado] || 'bi-circle-fill';
  }
}
