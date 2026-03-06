import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CampoPdf {
  nombre: string;
  tipo: string;           // Código PDF crudo: 'Tx', 'Btn', 'Ch'
  tipoDetallado?: string; // Tipo semántico: 'text', 'checkbox', 'choice'
  valor?: string;
  opciones?: string[];
}

export interface FormularioPdf {
  campos: CampoPdf[];
}

@Injectable({
  providedIn: 'root'
})
export class PdfEditorService {
  private apiUrl = environment.apiUrls.solicitudes;

  constructor(private http: HttpClient) {}

  obtenerCamposPdf(solicitudId: number): Observable<FormularioPdf> {
    return this.http.get<FormularioPdf>(`${this.apiUrl}/${solicitudId}/pdf/campos`);
  }

  rellenarCamposPdf(solicitudId: number, campos: Record<string, string>): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/${solicitudId}/pdf/rellenar`, campos, {
      responseType: 'blob'
    });
  }

  obtenerPdfBase64(solicitudId: number): Observable<{ base64: string }> {
    return this.http.get<{ base64: string }>(`${this.apiUrl}/${solicitudId}/pdf/preview`);
  }

  // Métodos para trabajar con plantillas (antes de crear el contrato)
  obtenerCamposPlantilla(tipoSolicitudId: number): Observable<FormularioPdf> {
    return this.http.get<FormularioPdf>(`${this.apiUrl}/plantilla/${tipoSolicitudId}/campos`);
  }

  obtenerPlantillaBase64(tipoSolicitudId: number): Observable<{ base64: string }> {
    return this.http.get<{ base64: string }>(`${this.apiUrl}/plantilla/${tipoSolicitudId}/preview`);
  }
}
