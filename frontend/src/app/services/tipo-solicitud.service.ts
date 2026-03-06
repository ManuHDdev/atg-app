import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TipoSolicitud } from '../models/tipo-solicitud.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TipoSolicitudService {
  private apiUrl = environment.apiUrls.tiposSolicitud;

  constructor(private http: HttpClient) {}

  getAll(): Observable<TipoSolicitud[]> {
    return this.http.get<TipoSolicitud[]>(this.apiUrl);
  }

  getById(id: string): Observable<TipoSolicitud> {
    return this.http.get<TipoSolicitud>(`${this.apiUrl}/${id}`);
  }

  getByPetroleraId(petroleraId: string): Observable<TipoSolicitud[]> {
    return this.http.get<TipoSolicitud[]>(`${this.apiUrl}/petrolera/${petroleraId}`);
  }

  getActivasByPetroleraId(petroleraId: string): Observable<TipoSolicitud[]> {
    return this.http.get<TipoSolicitud[]>(`${this.apiUrl}/petrolera/${petroleraId}/activas`);
  }

  create(tipoSolicitud: TipoSolicitud): Observable<TipoSolicitud> {
    return this.http.post<TipoSolicitud>(this.apiUrl, tipoSolicitud);
  }

  update(id: string, tipoSolicitud: TipoSolicitud): Observable<TipoSolicitud> {
    return this.http.put<TipoSolicitud>(`${this.apiUrl}/${id}`, tipoSolicitud);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  subirPlantillaPdf(id: string, archivo: File): Observable<TipoSolicitud> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<TipoSolicitud>(`${this.apiUrl}/${id}/plantilla`, formData);
  }

  eliminarPlantillaPdf(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/plantilla`);
  }

  obtenerUrlPlantillaPdf(id: string): string {
    return `${this.apiUrl}/${id}/plantilla`;
  }
}
