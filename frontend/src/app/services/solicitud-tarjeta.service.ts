import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SolicitudTarjeta, CrearSolicitudDTO, RegistrarLlegadaDTO, MarcarEntregadaDTO, AprobarBajaDTO, AprobarDuplicadoDTO } from '../models/solicitud-tarjeta.model';

@Injectable({
  providedIn: 'root'
})
export class SolicitudTarjetaService {
  private apiUrl = environment.apiUrls.solicitudesTarjetas;

  constructor(private http: HttpClient) {}

  getAll(): Observable<SolicitudTarjeta[]> {
    return this.http.get<SolicitudTarjeta[]>(this.apiUrl);
  }

  getById(id: string): Observable<SolicitudTarjeta> {
    return this.http.get<SolicitudTarjeta>(`${this.apiUrl}/${id}`);
  }

  getBySocioId(socioId: string): Observable<SolicitudTarjeta[]> {
    return this.http.get<SolicitudTarjeta[]>(`${this.apiUrl}/socio/${socioId}`);
  }

  getByEstado(estado: string): Observable<SolicitudTarjeta[]> {
    return this.http.get<SolicitudTarjeta[]>(`${this.apiUrl}/estado/${estado}`);
  }

  create(solicitud: CrearSolicitudDTO): Observable<SolicitudTarjeta> {
    return this.http.post<SolicitudTarjeta>(this.apiUrl, solicitud);
  }

  completar(id: string, procesadoPor?: string): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/completar`,
      { procesadoPor: procesadoPor || 'Admin' }
    );
  }

  rechazar(id: string, motivo: string, procesadoPor?: string): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/rechazar`,
      { motivo, procesadoPor: procesadoPor || 'Admin' }
    );
  }

  aprobar(id: string, procesadoPor?: string): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/aprobar`,
      { procesadoPor: procesadoPor || 'Admin' }
    );
  }

  aprobarBaja(id: string, data: AprobarBajaDTO): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/aprobar-baja`,
      data
    );
  }

  aprobarDuplicado(id: string, data: AprobarDuplicadoDTO): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/aprobar-duplicado`,
      data
    );
  }

  registrarLlegada(id: string, data: RegistrarLlegadaDTO): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/registrar-llegada`,
      data
    );
  }

  marcarEntregada(id: string, data: MarcarEntregadaDTO): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/marcar-entregada`,
      data
    );
  }

  finalizar(id: string): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/finalizar`,
      {}
    );
  }
}
