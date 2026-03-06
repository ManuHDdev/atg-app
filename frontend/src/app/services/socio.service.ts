import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Socio } from '../models/socio.model';

@Injectable({
  providedIn: 'root'
})
export class SocioService {
  private apiUrl = environment.apiUrls.socios;

  constructor(private http: HttpClient) { }

  getAll(): Observable<Socio[]> {
    return this.http.get<Socio[]>(this.apiUrl);
  }

  getById(id: string): Observable<Socio> {
    return this.http.get<Socio>(`${this.apiUrl}/${id}`);
  }

  create(socio: Socio): Observable<Socio> {
    return this.http.post<Socio>(this.apiUrl, socio);
  }

  update(id: string, socio: Socio): Observable<Socio> {
    return this.http.put<Socio>(`${this.apiUrl}/${id}`, socio);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
