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

interface BootstrapOffcanvasApi {
  getInstance(element: Element): { hide(): void } | null;
  getOrCreateInstance(element: Element): { hide(): void };
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    NgClass,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    TranslatePipe,
    LanguageSwitcherComponent,
    ToastContainerComponent
  ],
  templateUrl: './main-layout.component.html',
  styles: [
    `
      .organization-logo,
      .organization-logo-fallback {
        width: 34px;
        height: 34px;
        min-width: 34px;
        border-radius: 0.6rem;
      }

      .organization-logo {
        object-fit: cover;
        border: 1px solid rgba(0, 0, 0, 0.08);
        background: #fff;
      }

      .organization-logo-fallback {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border: 1px solid rgba(13, 110, 253, 0.18);
        background: rgba(13, 110, 253, 0.08);
        color: #0d6efd;
        font-size: 0.72rem;
        font-weight: 700;
        letter-spacing: 0.02em;
        text-transform: uppercase;
      }
    `
  ]
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

  readonly organizationLogoUrl = computed(() => this.user()?.organization?.logoUrl ?? null);

  readonly organizationInitials = computed(() => {
    const organizationName = this.user()?.organization?.name?.trim();
    if (!organizationName) {
      return 'ORG';
    }

    const initials = organizationName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((word) => word.charAt(0).toUpperCase())
      .join('');

    return initials || 'ORG';
  });

  readonly isAdministrator = computed(() => this.user()?.role === 'ADMINISTRATOR');
  readonly isSuperAdmin = computed(() => this.user()?.role === 'SUPER_ADMIN');

  readonly canManageOrganizations = computed(() => this.isSuperAdmin());
  readonly canManageUsers = computed(() => this.isAdministrator() || this.isSuperAdmin());

  readonly menuItems = computed(() => {
    const items: MenuItem[] = [
      { labelKey: 'navigation.profile', icon: 'bi-person-circle', route: '/profile' },
      { labelKey: 'navigation.dashboard', icon: 'bi-speedometer2', route: '/dashboard' },
      { labelKey: 'navigation.properties', icon: 'bi-houses', route: '/properties' },
      { labelKey: 'navigation.catalogs', icon: 'bi-tags', route: '/catalogs' },
      { labelKey: 'navigation.productBoxModels', icon: 'bi-box-seam', route: '/product-box-models' },
      { labelKey: 'navigation.maintenance', icon: 'bi-tools', route: '/maintenance' },
      { labelKey: 'navigation.scheduledMaintenance', icon: 'bi-calendar-check', route: '/scheduled-maintenance' },
      { labelKey: 'navigation.reservations', icon: 'bi-calendar2-week', route: '/reservations' },
      { labelKey: 'navigation.tasks', icon: 'bi-check2-square', route: '/tasks' },
      { labelKey: 'navigation.purchases', icon: 'bi-cart-check', route: '/purchases' },
      { labelKey: 'navigation.documents', icon: 'bi-file-earmark-text', route: '/documents' },
      { labelKey: 'navigation.aiAssistant', icon: 'bi-stars', route: '/ai-assistant' }
    ];

    if (this.canManageOrganizations()) {
      items.push({ labelKey: 'navigation.organizations', icon: 'bi-buildings', route: '/organizations' });
    }

    if (this.canManageUsers()) {
      items.push({ labelKey: 'navigation.users', icon: 'bi-people', route: '/users' });
    }

    return items;
  });

  closeMobileSidebar(): void {
    const sidebarElement = document.getElementById('mobileSidebar');
    if (!sidebarElement) {
      return;
    }

    const windowWithBootstrap = window as Window & { bootstrap?: { Offcanvas?: BootstrapOffcanvasApi } };
    const offcanvasApi = windowWithBootstrap.bootstrap?.Offcanvas;

    if (!offcanvasApi) {
      sidebarElement.classList.remove('show');
      document.querySelectorAll('.offcanvas-backdrop').forEach((backdrop) => backdrop.remove());
      document.body.style.removeProperty('overflow');
      document.body.style.removeProperty('padding-right');
      return;
    }

    const instance = offcanvasApi.getInstance(sidebarElement) ?? offcanvasApi.getOrCreateInstance(sidebarElement);
    instance.hide();
  }

  logout(): void {
    this.authService.logout();
  }
}
