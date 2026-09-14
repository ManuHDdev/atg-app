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

  // ATG no aprueba ni rechaza: registra la respuesta que ha dado la petrolera.
  // Quien tramita lo resuelve el backend desde el token: no se envia desde el cliente.
  denegarPorPetrolera(id: string, motivo: string): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/denegar-petrolera`,
      { motivo }
    );
  }

  aprobarPorPetrolera(id: string): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/aprobar-petrolera`,
      {}
    );
  }

  aprobarBajaPorPetrolera(id: string, data: AprobarBajaDTO): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/aprobar-baja-petrolera`,
      data
    );
  }

  aprobarDuplicadoPorPetrolera(id: string, data: AprobarDuplicadoDTO): Observable<SolicitudTarjeta> {
    return this.http.put<SolicitudTarjeta>(
      `${this.apiUrl}/${id}/aprobar-duplicado-petrolera`,
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
}
