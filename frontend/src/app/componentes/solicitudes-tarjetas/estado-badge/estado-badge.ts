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
      'PENDIENTE': '⏳',
      'APROBADA': '✓',
      'RECHAZADA': '✗',
      'TARJETA_LLEGADA': '📦',
      'ENTREGADA': '✓✓',
      'COMPLETADA': '✓✓✓'
    };
    return icons[this.estado] || '•';
  }
}
