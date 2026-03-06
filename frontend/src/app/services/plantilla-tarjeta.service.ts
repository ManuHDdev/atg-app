import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PlantillaTarjeta } from '../models/plantilla-tarjeta.model';

@Injectable({
  providedIn: 'root'
})
export class PlantillaTarjetaService {
  private apiUrl = environment.apiUrls.plantillasTarjetas;

  constructor(private http: HttpClient) {}

  getAll(): Observable<PlantillaTarjeta[]> {
    return this.http.get<PlantillaTarjeta[]>(this.apiUrl);
  }

  getById(id: string): Observable<PlantillaTarjeta> {
    return this.http.get<PlantillaTarjeta>(`${this.apiUrl}/${id}`);
  }

  getByTipo(tipo: string): Observable<PlantillaTarjeta> {
    return this.http.get<PlantillaTarjeta>(`${this.apiUrl}/tipo/${tipo}`);
  }

  create(plantilla: PlantillaTarjeta): Observable<PlantillaTarjeta> {
    return this.http.post<PlantillaTarjeta>(this.apiUrl, plantilla);
  }

  update(id: string, plantilla: PlantillaTarjeta): Observable<PlantillaTarjeta> {
    return this.http.put<PlantillaTarjeta>(`${this.apiUrl}/${id}`, plantilla);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
