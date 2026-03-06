import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Petrolera } from '../models/petrolera.model';

@Injectable({
  providedIn: 'root'
})
export class PetroleraService {
  private apiUrl = environment.apiUrls.petroleras;

  constructor(private http: HttpClient) { }

  getAll(): Observable<Petrolera[]> {
    return this.http.get<Petrolera[]>(this.apiUrl);
  }

  listar(): Observable<Petrolera[]> {
    return this.http.get<Petrolera[]>(this.apiUrl);
  }

  getById(id: string): Observable<Petrolera> {
    return this.http.get<Petrolera>(`${this.apiUrl}/${id}`);
  }

  getActivas(): Observable<Petrolera[]> {
    return this.http.get<Petrolera[]>(`${this.apiUrl}?activa=true`);
  }

  create(petrolera: Petrolera): Observable<Petrolera> {
    return this.http.post<Petrolera>(this.apiUrl, petrolera);
  }

  update(id: string, petrolera: Petrolera): Observable<Petrolera> {
    return this.http.put<Petrolera>(`${this.apiUrl}/${id}`, petrolera);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
