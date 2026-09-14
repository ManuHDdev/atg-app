import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Dispositivo,
  SolicitudDispositivo,
  CrearSolicitudDispositivoDTO,
  EstadoSolicitudDispositivo
} from '../models/dispositivo.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DispositivoService {
  private apiUrl = environment.apiUrls.dispositivos;
  private solicitudUrl = environment.apiUrls.solicitudesDispositivo;

  constructor(private http: HttpClient) { }

  // Dispositivos
  listarPorSocio(socioId: number): Observable<Dispositivo[]> {
    return this.http.get<Dispositivo[]>(`${this.apiUrl}/socio/${socioId}`);
  }

  listarActivosPorSocio(socioId: number): Observable<Dispositivo[]> {
    return this.http.get<Dispositivo[]>(`${this.apiUrl}/socio/${socioId}/activos`);
  }

  obtenerPorId(id: number): Observable<Dispositivo> {
    return this.http.get<Dispositivo>(`${this.apiUrl}/${id}`);
  }

  // Solicitudes
  listarSolicitudes(): Observable<SolicitudDispositivo[]> {
    return this.http.get<SolicitudDispositivo[]>(this.solicitudUrl);
  }

  listarSolicitudesPorSocio(socioId: number): Observable<SolicitudDispositivo[]> {
    return this.http.get<SolicitudDispositivo[]>(`${this.solicitudUrl}/socio/${socioId}`);
  }

  obtenerSolicitudPorId(id: number): Observable<SolicitudDispositivo> {
    return this.http.get<SolicitudDispositivo>(`${this.solicitudUrl}/${id}`);
  }

  listarPorEstado(estado: EstadoSolicitudDispositivo): Observable<SolicitudDispositivo[]> {
    return this.http.get<SolicitudDispositivo[]>(`${this.solicitudUrl}/estado/${estado}`);
  }

  crearSolicitud(dto: CrearSolicitudDispositivoDTO): Observable<SolicitudDispositivo> {
    return this.http.post<SolicitudDispositivo>(this.solicitudUrl, dto);
  }

  enviarAPetrolera(id: number): Observable<SolicitudDispositivo> {
    return this.http.post<SolicitudDispositivo>(`${this.solicitudUrl}/${id}/enviar-petrolera`, {});
  }

  responderPetrolera(id: number, aprobado: boolean, respuesta: string, montoConcedido?: number | null): Observable<SolicitudDispositivo> {
    return this.http.post<SolicitudDispositivo>(`${this.solicitudUrl}/${id}/responder`, {
      aprobado,
      respuesta,
      montoConcedido: aprobado ? montoConcedido ?? null : null
    });
  }

  notificarSocio(id: number): Observable<SolicitudDispositivo> {
    return this.http.post<SolicitudDispositivo>(`${this.solicitudUrl}/${id}/notificar-socio`, {});
  }
}
