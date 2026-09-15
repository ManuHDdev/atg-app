import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ModuloDocumento, PlantillaDocumento } from '../models/plantilla-documento.model';

@Injectable({
  providedIn: 'root'
})
export class PlantillaDocumentoService {
  private apiUrl = environment.apiUrls.plantillasDocumento;

  constructor(private http: HttpClient) {}

  listarTodas(): Observable<PlantillaDocumento[]> {
    return this.http.get<PlantillaDocumento[]>(this.apiUrl);
  }

  listarPorPetrolera(petroleraId: number): Observable<PlantillaDocumento[]> {
    return this.http.get<PlantillaDocumento[]>(`${this.apiUrl}/petrolera/${petroleraId}`);
  }

  obtenerPorId(id: number): Observable<PlantillaDocumento> {
    return this.http.get<PlantillaDocumento>(`${this.apiUrl}/${id}`);
  }

  buscar(petroleraId: number, modulo: ModuloDocumento, tipoSolicitud: string): Observable<PlantillaDocumento> {
    const params = new HttpParams()
      .set('petroleraId', petroleraId)
      .set('modulo', modulo)
      .set('tipoSolicitud', tipoSolicitud);
    return this.http.get<PlantillaDocumento>(`${this.apiUrl}/buscar`, { params });
  }

  descargarArchivo(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/archivo`, { responseType: 'blob' });
  }

  crear(petroleraId: number, modulo: ModuloDocumento, tipoSolicitud: string, archivo: File): Observable<PlantillaDocumento> {
    const formData = new FormData();
    formData.append('file', archivo);
    formData.append('petroleraId', String(petroleraId));
    formData.append('modulo', modulo);
    formData.append('tipoSolicitud', tipoSolicitud);
    return this.http.post<PlantillaDocumento>(this.apiUrl, formData);
  }

  reemplazarArchivo(id: number, archivo: File): Observable<PlantillaDocumento> {
    const formData = new FormData();
    formData.append('file', archivo);
    return this.http.put<PlantillaDocumento>(`${this.apiUrl}/${id}`, formData);
  }

  cambiarEstado(id: number, activa: boolean): Observable<PlantillaDocumento> {
    return this.http.patch<PlantillaDocumento>(`${this.apiUrl}/${id}/estado`, { activa });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
