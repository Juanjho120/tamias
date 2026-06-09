import { Injectable, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { LanguageOption, SupportedLanguage } from './language.model';

const LANGUAGE_KEY = 'tamias_language';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  readonly languages: LanguageOption[] = [
    {
      code: 'en',
      label: 'English',
      shortLabel: 'EN'
    },
    {
      code: 'es',
      label: 'Español',
      shortLabel: 'ES'
    }
  ];

  private readonly currentLanguageSignal = signal<SupportedLanguage>(this.getInitialLanguage());
  readonly currentLanguage = this.currentLanguageSignal.asReadonly();

  constructor(private readonly translateService: TranslateService) {
    this.translateService.addLangs(this.languages.map((language) => language.code));
    this.translateService.setFallbackLang('en');
    this.use(this.currentLanguageSignal());
  }

  use(language: SupportedLanguage): void {
    localStorage.setItem(LANGUAGE_KEY, language);
    document.documentElement.lang = language;
    this.currentLanguageSignal.set(language);
    this.translateService.use(language);
  }

  instant(key: string, params?: Record<string, unknown>): string {
    return this.translateService.instant(key, params);
  }

  private getInitialLanguage(): SupportedLanguage {
    const storedLanguage = localStorage.getItem(LANGUAGE_KEY);

    if (storedLanguage === 'en' || storedLanguage === 'es') {
      return storedLanguage;
    }

    const browserLanguage = navigator.language.toLowerCase();

    if (browserLanguage.startsWith('es')) {
      return 'es';
    }

    return 'en';
  }
}
