import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Credito, CrearCreditoDTO, EstadoCredito } from '../models/credito.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CreditoService {
  private apiUrl = environment.apiUrls.creditos;

  constructor(private http: HttpClient) { }

  listarTodos(): Observable<Credito[]> {
    return this.http.get<Credito[]>(this.apiUrl);
  }

  obtenerPorId(id: number): Observable<Credito> {
    return this.http.get<Credito>(`${this.apiUrl}/${id}`);
  }

  listarPorSocio(socioId: number): Observable<Credito[]> {
    return this.http.get<Credito[]>(`${this.apiUrl}/socio/${socioId}`);
  }

  listarPorPetrolera(petroleraId: number): Observable<Credito[]> {
    return this.http.get<Credito[]>(`${this.apiUrl}/petrolera/${petroleraId}`);
  }

  listarPorEstado(estado: EstadoCredito): Observable<Credito[]> {
    return this.http.get<Credito[]>(`${this.apiUrl}/estado/${estado}`);
  }

  crear(dto: CrearCreditoDTO): Observable<Credito> {
    return this.http.post<Credito>(this.apiUrl, dto);
  }

  enviarAPetrolera(id: number): Observable<Credito> {
    return this.http.post<Credito>(`${this.apiUrl}/${id}/enviar-petrolera`, {});
  }

  responderPetrolera(id: number, aprobado: boolean, respuesta: string): Observable<Credito> {
    return this.http.post<Credito>(`${this.apiUrl}/${id}/responder`, {
      aprobado,
      respuesta
    });
  }

  notificarSocio(id: number): Observable<Credito> {
    return this.http.post<Credito>(`${this.apiUrl}/${id}/notificar-socio`, {});
  }
}
