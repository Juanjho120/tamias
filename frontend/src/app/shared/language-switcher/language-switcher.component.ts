import { Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { SupportedLanguage } from '../../core/i18n/language.model';
import { LanguageService } from '../../core/i18n/language.service';

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './language-switcher.component.html'
})
export class LanguageSwitcherComponent {
  readonly languageService = inject(LanguageService);

  changeLanguage(language: SupportedLanguage): void {
    this.languageService.use(language);
  }
}
