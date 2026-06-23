import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideTranslateService } from '@ngx-translate/core';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { IconActionButtonAutoEnhancerService } from './shared/icon-action-button/icon-action-button-auto-enhancer.service';
import { TamiBrandingService } from './shared/tami-robot/tami-branding.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideTranslateService({
      fallbackLang: 'en',
      lang: localStorage.getItem('tamias_language') || 'en',
      loader: provideTranslateHttpLoader({
        prefix: '/assets/i18n/',
        suffix: '.json'
      })
    }),
    provideAppInitializer(() => {
      const iconActionEnhancer = inject(IconActionButtonAutoEnhancerService);
      iconActionEnhancer.start();

      const tamiBranding = inject(TamiBrandingService);
      tamiBranding.start();
    })
  ]
};
