import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TipoContrato } from '../models/tipo-contrato.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TipoContratoService {
  private apiUrl = environment.apiUrls.tiposContrato;

  constructor(private http: HttpClient) {}

  getAll(): Observable<TipoContrato[]> {
    return this.http.get<TipoContrato[]>(this.apiUrl);
  }

  listar(): Observable<TipoContrato[]> {
    return this.http.get<TipoContrato[]>(this.apiUrl);
  }

  getActivos(): Observable<TipoContrato[]> {
    return this.http.get<TipoContrato[]>(`${this.apiUrl}/activos`);
  }

  getById(id: number): Observable<TipoContrato> {
    return this.http.get<TipoContrato>(`${this.apiUrl}/${id}`);
  }

  getByCodigo(codigo: string): Observable<TipoContrato> {
    return this.http.get<TipoContrato>(`${this.apiUrl}/codigo/${codigo}`);
  }

  create(tipo: TipoContrato): Observable<TipoContrato> {
    return this.http.post<TipoContrato>(this.apiUrl, tipo);
  }

  update(id: number, tipo: TipoContrato): Observable<TipoContrato> {
    return this.http.put<TipoContrato>(`${this.apiUrl}/${id}`, tipo);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  activar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/activar`, {});
  }

  desactivar(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/desactivar`, {});
  }
}
