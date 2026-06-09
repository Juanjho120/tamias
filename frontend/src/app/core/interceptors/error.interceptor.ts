import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../models/api-error.model';
import { LanguageService } from '../i18n/language.service';
import { ToastService } from '../../shared/toast/toast.service';

export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const toastService = inject(ToastService);
  const languageService = inject(LanguageService);

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      if (shouldShowGlobalError(error)) {
        toastService.error(resolveErrorMessage(error, languageService));
      }

      return throwError(() => error);
    })
  );
};

function shouldShowGlobalError(error: HttpErrorResponse): boolean {
  if (error.status === 0 || error.status === 403 || error.status >= 500) {
    return true;
  }

  return false;
}

function resolveErrorMessage(error: HttpErrorResponse, languageService: LanguageService): string {
  if (error.status === 0) {
    return languageService.instant('errors.network');
  }

  if (error.status === 403) {
    return languageService.instant('errors.forbidden');
  }

  const apiError = error.error as ApiError | undefined;

  if (error.status >= 500) {
    return apiError?.message || languageService.instant('errors.server');
  }

  return apiError?.message || languageService.instant('errors.unexpected');
}
