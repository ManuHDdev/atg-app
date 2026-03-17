import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';
import { ErrorHandlerService } from '../services/error-handler.service';

/**
 * Interceptor global de errores HTTP.
 * - 5xx / conexión (0): muestra toast automáticamente y re-lanza.
 * - 4xx: re-lanza el error enriquecido para que el componente lo maneje en contexto.
 * - 401: el componente puede redirigir al login si lo necesita.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);
  const errorHandler = inject(ErrorHandlerService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // Errores de servidor o de conexión → toast automático
      if (err.status === 0 || err.status >= 500) {
        notifications.error(errorHandler.getMensaje(err), 5000);
      }
      // Re-lanzar siempre para que el componente pueda reaccionar (loading=false, etc.)
      return throwError(() => err);
    })
  );
};
