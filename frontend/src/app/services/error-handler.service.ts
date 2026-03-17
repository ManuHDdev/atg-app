import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {

  /**
   * Extrae el mensaje de error más descriptivo posible.
   * Prioridad:
   *  1. validationErrors del backend (los une en una frase)
   *  2. message del backend (ErrorResponse.message)
   *  3. Mensaje según código HTTP
   *  4. Mensaje genérico
   */
  getMensaje(err: HttpErrorResponse | any, contexto?: string): string {
    // 1. Errores de validación de campo: mostrar todos unidos
    const validationErrors = err?.error?.validationErrors;
    if (validationErrors && typeof validationErrors === 'object') {
      const mensajes = Object.values(validationErrors) as string[];
      if (mensajes.length > 0) {
        return mensajes.join('. ');
      }
    }

    // 2. Mensaje del backend
    const backendMessage = err?.error?.message;
    if (backendMessage && typeof backendMessage === 'string' && backendMessage.trim()) {
      return backendMessage;
    }

    // 3. Código HTTP
    switch (err?.status) {
      case 0:   return 'No se puede conectar con el servidor. Comprueba tu conexión.';
      case 400: return contexto ? `Datos incorrectos en ${contexto}. Revisa el formulario.` : 'Los datos enviados son incorrectos.';
      case 401: return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
      case 403: return 'No tienes permiso para realizar esta acción.';
      case 404: return contexto ? `${contexto} no encontrado.` : 'El recurso solicitado no existe.';
      case 409: return contexto ? `Ya existe un ${contexto} con esos datos.` : 'Ya existe un registro con esos datos.';
      case 413: return 'El archivo es demasiado grande. El tamaño máximo es 50 MB.';
      case 422: return 'Los datos no cumplen las reglas de negocio.';
      case 500: return 'Error interno del servidor. Inténtalo de nuevo en unos momentos.';
      case 503: return 'Servicio no disponible. Inténtalo de nuevo más tarde.';
    }

    // 4. Genérico
    return 'Ha ocurrido un error inesperado. Inténtalo de nuevo.';
  }

  /**
   * Devuelve el mapa de errores de validación de campo (campo → mensaje).
   * Útil para marcar campos individuales del formulario.
   */
  getValidationErrors(err: HttpErrorResponse | any): Record<string, string> {
    return err?.error?.validationErrors ?? {};
  }
}
