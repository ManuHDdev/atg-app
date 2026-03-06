import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Tarjeta } from '../models/tarjeta.model';

@Injectable({
  providedIn: 'root'
})
export class TarjetaService {
  private apiUrl = environment.apiUrls.tarjetas;

  constructor(private http: HttpClient) { }

  getAll(): Observable<Tarjeta[]> {
    return this.http.get<Tarjeta[]>(this.apiUrl);
  }

  getById(id: string): Observable<Tarjeta> {
    return this.http.get<Tarjeta>(`${this.apiUrl}/${id}`);
  }

  getBySocioId(socioId: string): Observable<Tarjeta[]> {
    return this.http.get<Tarjeta[]>(`${this.apiUrl}/socio/${socioId}`);
  }

  getByMatricula(matricula: string): Observable<Tarjeta[]> {
    return this.http.get<Tarjeta[]>(`${this.apiUrl}/matricula/${matricula}`);
  }

  create(tarjeta: Tarjeta): Observable<Tarjeta> {
    return this.http.post<Tarjeta>(this.apiUrl, tarjeta);
  }

  update(id: string, tarjeta: Tarjeta): Observable<Tarjeta> {
    return this.http.put<Tarjeta>(`${this.apiUrl}/${id}`, tarjeta);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
