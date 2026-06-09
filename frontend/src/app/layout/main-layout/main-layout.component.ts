import { NgClass } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher.component';
import { ToastContainerComponent } from '../../shared/toast/toast-container.component';

interface MenuItem {
  labelKey: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [NgClass, RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe, LanguageSwitcherComponent, ToastContainerComponent],
  templateUrl: './main-layout.component.html'
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);

  readonly user = this.authService.currentUser;

  readonly displayName = computed(() => {
    const user = this.user();

    if (!user) {
      return 'User';
    }

    return `${user.firstName} ${user.lastName}`;
  });

  readonly menuItems: MenuItem[] = [
    { labelKey: 'navigation.dashboard', icon: 'bi-speedometer2', route: '/dashboard' },
    { labelKey: 'navigation.properties', icon: 'bi-houses', route: '/properties' },
    { labelKey: 'navigation.catalogs', icon: 'bi-tags', route: '/catalogs' },
    { labelKey: 'navigation.maintenance', icon: 'bi-tools', route: '/maintenance' },
    { labelKey: 'navigation.scheduledMaintenance', icon: 'bi-calendar-check', route: '/scheduled-maintenance' },
    { labelKey: 'navigation.reservations', icon: 'bi-calendar2-week', route: '/reservations' },
    { labelKey: 'navigation.tasks', icon: 'bi-check2-square', route: '/tasks' },
    { labelKey: 'navigation.purchases', icon: 'bi-cart-check', route: '/purchases' },
    { labelKey: 'navigation.documents', icon: 'bi-file-earmark-text', route: '/documents' },
    { labelKey: 'navigation.aiAssistant', icon: 'bi-stars', route: '/ai-assistant' }
  ];

  logout(): void {
    this.authService.logout();
  }
}
