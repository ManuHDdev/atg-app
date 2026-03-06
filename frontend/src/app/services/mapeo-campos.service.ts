import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { MapeoPlantillaCampos } from '../models/mapeo-campos.model';

@Injectable({
  providedIn: 'root'
})
export class MapeoCamposService {
  private apiUrl = environment.apiUrls.mapeoCampos;

  constructor(private http: HttpClient) { }

  getAll(): Observable<MapeoPlantillaCampos[]> {
    return this.http.get<MapeoPlantillaCampos[]>(this.apiUrl);
  }

  getById(id: string): Observable<MapeoPlantillaCampos> {
    return this.http.get<MapeoPlantillaCampos>(`${this.apiUrl}/${id}`);
  }

  getByPlantillaId(plantillaId: string): Observable<MapeoPlantillaCampos[]> {
    return this.http.get<MapeoPlantillaCampos[]>(`${this.apiUrl}?plantillaId=${plantillaId}`);
  }

  create(mapeo: MapeoPlantillaCampos): Observable<MapeoPlantillaCampos> {
    return this.http.post<MapeoPlantillaCampos>(this.apiUrl, mapeo);
  }

  update(id: string, mapeo: MapeoPlantillaCampos): Observable<MapeoPlantillaCampos> {
    return this.http.put<MapeoPlantillaCampos>(`${this.apiUrl}/${id}`, mapeo);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
