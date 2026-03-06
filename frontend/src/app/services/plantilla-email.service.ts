import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { PlantillaEmail, CrearPlantillaEmailDTO, TipoEventoEmail } from '../models/plantilla-email.model';

// Forma del DTO del backend (petroleras service)
interface PlantillaCorreoBackend {
  id?: number;
  petroleraId: number;
  petroleraNombre?: string;
  tipoPlantilla: string;
  asunto: string;
  cuerpo: string;
  variablesDisponibles?: string;
  activa: boolean;
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlantillaEmailService {
  private apiUrl = environment.apiUrls.plantillasCorreo;

  constructor(private http: HttpClient) {}

  private mapFromBackend(backend: PlantillaCorreoBackend): PlantillaEmail {
    return {
      id: backend.id,
      nombre: (backend.petroleraNombre || 'Sin petrolera') + ' - ' + backend.tipoPlantilla,
      asunto: backend.asunto,
      cuerpo: backend.cuerpo,
      tipoEvento: backend.tipoPlantilla as TipoEventoEmail,
      activa: backend.activa,
      variables: backend.variablesDisponibles,
      petroleraId: backend.petroleraId,
      petroleraNombre: backend.petroleraNombre,
      createdAt: backend.createdAt,
      updatedAt: backend.updatedAt
    };
  }

  private mapToBackend(plantilla: CrearPlantillaEmailDTO): PlantillaCorreoBackend {
    return {
      petroleraId: plantilla.petroleraId || 0,
      tipoPlantilla: plantilla.tipoEvento,
      asunto: plantilla.asunto,
      cuerpo: plantilla.cuerpo,
      variablesDisponibles: '',
      activa: plantilla.activa
    };
  }

  listarTodas(): Observable<PlantillaEmail[]> {
    return this.http.get<PlantillaCorreoBackend[]>(this.apiUrl).pipe(
      map(items => items.map(item => this.mapFromBackend(item)))
    );
  }

  obtenerPorId(id: number): Observable<PlantillaEmail> {
    return this.http.get<PlantillaCorreoBackend>(`${this.apiUrl}/${id}`).pipe(
      map(item => this.mapFromBackend(item))
    );
  }

  obtenerPorPetrolera(petroleraId: number): Observable<PlantillaEmail[]> {
    return this.http.get<PlantillaCorreoBackend[]>(`${this.apiUrl}/petrolera/${petroleraId}`).pipe(
      map(items => items.map(item => this.mapFromBackend(item)))
    );
  }

  crear(plantilla: CrearPlantillaEmailDTO): Observable<PlantillaEmail> {
    return this.http.post<PlantillaCorreoBackend>(this.apiUrl, this.mapToBackend(plantilla)).pipe(
      map(item => this.mapFromBackend(item))
    );
  }

  actualizar(id: number, plantilla: CrearPlantillaEmailDTO): Observable<PlantillaEmail> {
    return this.http.put<PlantillaCorreoBackend>(`${this.apiUrl}/${id}`, this.mapToBackend(plantilla)).pipe(
      map(item => this.mapFromBackend(item))
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  cambiarEstado(id: number, activa: boolean): Observable<PlantillaEmail> {
    return this.http.patch<PlantillaCorreoBackend>(`${this.apiUrl}/${id}/estado`, { activa }).pipe(
      map(item => this.mapFromBackend(item))
    );
  }
}
