import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-email-logs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './email-logs.html',
  styleUrl: './email-logs.css'
})
export class EmailLogs {
  @Input() correosEnviados: string = '';

  get hasEmails(): boolean {
    return !!this.correosEnviados && this.correosEnviados.trim().length > 0;
  }

  get emailLines(): string[] {
    if (!this.correosEnviados) return [];
    return this.correosEnviados
      .split('\n')
      .filter(line => line.trim().length > 0);
  }

  isSuccess(line: string): boolean {
    return line.includes('ENVIADO') || line.includes('true');
  }

  isError(line: string): boolean {
    return line.includes('ERROR') || line.includes('false');
  }
}
