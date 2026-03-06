import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SolicitudContrato,
  CrearSolicitudDTO,
  FiltroSolicitudesDTO,
  EstadoSolicitud
} from '../models/solicitud-contrato.model';
import { environment } from '../../environments/environment';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class SolicitudContratoService {
  private apiUrl = environment.apiUrls.solicitudes;

  constructor(private http: HttpClient) {}

  crear(dto: CrearSolicitudDTO): Observable<SolicitudContrato> {
    return this.http.post<SolicitudContrato>(this.apiUrl, dto);
  }

  listar(filtros: FiltroSolicitudesDTO): Observable<PageResponse<SolicitudContrato>> {
    let params = new HttpParams();

    if (filtros.socioId) params = params.set('socioId', filtros.socioId.toString());
    if (filtros.petroleraId) params = params.set('petroleraId', filtros.petroleraId.toString());
    if (filtros.tipoContratoId) params = params.set('tipoContratoId', filtros.tipoContratoId.toString());
    if (filtros.estado) params = params.set('estado', filtros.estado);
    if (filtros.page !== undefined) params = params.set('page', filtros.page.toString());
    if (filtros.size !== undefined) params = params.set('size', filtros.size.toString());
    if (filtros.sortBy) params = params.set('sortBy', filtros.sortBy);
    if (filtros.sortDirection) params = params.set('sortDirection', filtros.sortDirection);

    return this.http.get<PageResponse<SolicitudContrato>>(this.apiUrl, { params });
  }

  obtenerPorId(id: number): Observable<SolicitudContrato> {
    return this.http.get<SolicitudContrato>(`${this.apiUrl}/${id}`);
  }

  obtenerPorNumero(numeroSolicitud: string): Observable<SolicitudContrato> {
    return this.http.get<SolicitudContrato>(`${this.apiUrl}/numero/${numeroSolicitud}`);
  }

  descargarPdfEditable(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf/editable`, {
      responseType: 'blob'
    });
  }

  descargarPlantillaOriginal(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf/plantilla-original`, {
      responseType: 'blob'
    });
  }

  guardarPdfEditado(id: number, archivo: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', archivo);
    return this.http.post<void>(`${this.apiUrl}/${id}/pdf/editable`, formData);
  }

  enviarASocio(id: number): Observable<SolicitudContrato> {
    return this.http.post<SolicitudContrato>(`${this.apiUrl}/${id}/enviar-socio`, {});
  }

  subirPdfFirmado(id: number, archivo: File): Observable<SolicitudContrato> {
    const formData = new FormData();
    formData.append('file', archivo);
    return this.http.post<SolicitudContrato>(`${this.apiUrl}/${id}/pdf/firmado`, formData);
  }

  enviarAPetrolera(id: number): Observable<SolicitudContrato> {
    return this.http.post<SolicitudContrato>(`${this.apiUrl}/${id}/enviar-petrolera`, {});
  }

  aceptarFirmaSocio(id: number): Observable<SolicitudContrato> {
    return this.http.post<SolicitudContrato>(`${this.apiUrl}/${id}/aceptar-firma`, {});
  }

  aceptarPorPetrolera(id: number): Observable<SolicitudContrato> {
    return this.http.post<SolicitudContrato>(`${this.apiUrl}/${id}/aceptar-petrolera`, {});
  }

  rechazarPorPetrolera(id: number, motivoRechazo: string): Observable<SolicitudContrato> {
    return this.http.post<SolicitudContrato>(`${this.apiUrl}/${id}/rechazar-petrolera`, { motivoRechazo });
  }

  descargarPdf(id: number, tipo: 'editable' | 'enviado' | 'firmado' | 'final'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf/${tipo}`, {
      responseType: 'blob'
    });
  }

  cambiarEstado(id: number, estado: EstadoSolicitud): Observable<SolicitudContrato> {
    return this.http.put<SolicitudContrato>(`${this.apiUrl}/${id}/estado`, null, {
      params: { estado }
    });
  }

  rellenarCamposPdf(id: number, campos: Record<string, string>): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/${id}/pdf/rellenar`, campos, {
      responseType: 'blob'
    });
  }
}
