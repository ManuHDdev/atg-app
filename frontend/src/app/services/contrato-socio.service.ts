import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ContratoSocio } from '../models/contrato-socio.model';

@Injectable({
  providedIn: 'root'
})
export class ContratoSocioService {
  private apiUrl = `${environment.apiUrlContratos}/api/contratos`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ContratoSocio[]> {
    return this.http.get<ContratoSocio[]>(this.apiUrl);
  }

  getById(id: number): Observable<ContratoSocio> {
    return this.http.get<ContratoSocio>(`${this.apiUrl}/${id}`);
  }

  getBySocioId(socioId: number): Observable<ContratoSocio[]> {
    return this.http.get<ContratoSocio[]>(`${this.apiUrl}/socio/${socioId}`);
  }

  getActivosBySocioAndPetrolera(socioId: number, petroleraId: number): Observable<ContratoSocio[]> {
    return this.http.get<ContratoSocio[]>(`${this.apiUrl}/socio/${socioId}/petrolera/${petroleraId}/activos`);
  }

  getPetrolerasActivasBySocio(socioId: number): Observable<number[]> {
    return this.http.get<number[]>(`${this.apiUrl}/socio/${socioId}/petroleras-activas`);
  }
}
