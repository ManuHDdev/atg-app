import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlantillaCorreo } from '../models/plantilla-correo.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PlantillaCorreoService {
  private apiUrl = environment.apiUrls.plantillasCorreo;

  constructor(private http: HttpClient) { }

  listarTodas(): Observable<PlantillaCorreo[]> {
    return this.http.get<PlantillaCorreo[]>(this.apiUrl);
  }

  listarPorPetrolera(petroleraId: number): Observable<PlantillaCorreo[]> {
    return this.http.get<PlantillaCorreo[]>(`${this.apiUrl}/petrolera/${petroleraId}`);
  }

  obtenerPorId(id: number): Observable<PlantillaCorreo> {
    return this.http.get<PlantillaCorreo>(`${this.apiUrl}/${id}`);
  }

  crear(plantilla: PlantillaCorreo): Observable<PlantillaCorreo> {
    return this.http.post<PlantillaCorreo>(this.apiUrl, plantilla);
  }

  actualizar(id: number, plantilla: PlantillaCorreo): Observable<PlantillaCorreo> {
    return this.http.put<PlantillaCorreo>(`${this.apiUrl}/${id}`, plantilla);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
