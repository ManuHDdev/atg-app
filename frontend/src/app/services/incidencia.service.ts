import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AddComentarioRequest, CambiarEstadoRequest,
  Comentario, CreateIncidenciaRequest,
  Incidencia, IncidenciaResumen
} from '../models/incidencia.model';

@Injectable({ providedIn: 'root' })
export class IncidenciaService {
  private apiUrl = environment.apiUrls.incidencias;

  constructor(private http: HttpClient) {}

  getAll(): Observable<IncidenciaResumen[]> {
    return this.http.get<IncidenciaResumen[]>(this.apiUrl);
  }

  getById(id: number): Observable<Incidencia> {
    return this.http.get<Incidencia>(`${this.apiUrl}/${id}`);
  }

  create(req: CreateIncidenciaRequest): Observable<Incidencia> {
    return this.http.post<Incidencia>(this.apiUrl, req);
  }

  cambiarEstado(id: number, req: CambiarEstadoRequest): Observable<Incidencia> {
    return this.http.put<Incidencia>(`${this.apiUrl}/${id}/estado`, req);
  }

  addComentario(id: number, req: AddComentarioRequest): Observable<Comentario> {
    return this.http.post<Comentario>(`${this.apiUrl}/${id}/comentarios`, req);
  }

  eliminarComentario(id: number, comentarioId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/comentarios/${comentarioId}`);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
