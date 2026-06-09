import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../models/api-error.model';
import { ToastService } from '../../shared/toast/toast.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const toastService = inject(ToastService);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (shouldShowGlobalError(error, request.url)) {
        toastService.error(resolveErrorMessage(error));
      }

      return throwError(() => error);
    })
  );
};

function shouldShowGlobalError(error: HttpErrorResponse, requestUrl: string): boolean {
  if (requestUrl.includes('/assets/i18n/')) {
    return false;
  }

  if (error.status === 0 || error.status === 403 || error.status >= 500) {
    return true;
  }

  return false;
}

function resolveErrorMessage(error: HttpErrorResponse): string {
  const language = getCurrentLanguage();

  if (error.status === 0) {
    return translateStatic(language, 'errors.network');
  }

  if (error.status === 403) {
    return translateStatic(language, 'errors.forbidden');
  }

  const apiError = error.error as ApiError | undefined;

  if (error.status >= 500) {
    return apiError?.message || translateStatic(language, 'errors.server');
  }

  return apiError?.message || translateStatic(language, 'errors.unexpected');
}

function getCurrentLanguage(): 'en' | 'es' {
  const storedLanguage = localStorage.getItem('tamias_language');

  if (storedLanguage === 'es') {
    return 'es';
  }

  return 'en';
}

function translateStatic(language: 'en' | 'es', key: string): string {
  const messages: Record<'en' | 'es', Record<string, string>> = {
    en: {
      'errors.network': 'Backend unavailable. Please verify that the API is running.',
      'errors.forbidden': 'You do not have permission to perform this action.',
      'errors.server': 'Unexpected internal server error.',
      'errors.unexpected': 'Unexpected error.'
    },
    es: {
      'errors.network': 'Backend no disponible. Verifica que la API esté levantada.',
      'errors.forbidden': 'No tienes permisos para realizar esta acción.',
      'errors.server': 'Error interno inesperado del servidor.',
      'errors.unexpected': 'Error inesperado.'
    }
  };

  return messages[language][key] || messages[language]['errors.unexpected'];
}
