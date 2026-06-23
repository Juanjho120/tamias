import { NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiError } from '../../core/models/api-error.model';
import { AuthOrganizationOption } from '../../core/models/auth.models';
import { LanguageService } from '../../core/i18n/language.service';
import { AuthService } from '../../core/services/auth.service';
import { LanguageSwitcherComponent } from '../../shared/language-switcher/language-switcher.component';
import { ToastContainerComponent } from '../../shared/toast/toast-container.component';
import { ToastService } from '../../shared/toast/toast.service';

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

    .organization-switcher {
      min-width: 180px;
      max-width: 260px;
    }

    .organization-switcher .form-select {
      min-height: 31px;
      padding-top: 0.2rem;
      padding-bottom: 0.2rem;
      font-size: 0.8125rem;
    }
    `
  ]
})
export class MainLayoutComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly user = this.authService.currentUser;
  readonly organizationOptions = signal<AuthOrganizationOption[]>([]);
  readonly loadingOrganizations = signal(false);
  readonly switchingOrganization = signal(false);
  readonly selectedOrganizationId = signal<string>('');

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
  readonly canSwitchOrganizations = computed(() => this.organizationOptions().length > 1);

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

  ngOnInit(): void {
    this.selectedOrganizationId.set(this.user()?.organization?.id ?? '');
    this.loadOrganizationOptions();
  }

  loadOrganizationOptions(): void {
    if (!this.user()) {
      return;
    }

    this.loadingOrganizations.set(true);
    this.authService.listOrganizations().subscribe({
      next: (organizations) => {
        this.organizationOptions.set(organizations);
        const currentOrganization = organizations.find((organization) => organization.current);
        this.selectedOrganizationId.set(currentOrganization?.id ?? this.user()?.organization?.id ?? '');
        this.loadingOrganizations.set(false);
      },
      error: (error: unknown) => {
        this.loadingOrganizations.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('organizationSwitcher.messages.loadError')));
      }
    });
  }

  switchOrganization(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const organizationId = select.value;

    if (!organizationId || organizationId === this.user()?.organization?.id || this.switchingOrganization()) {
      this.selectedOrganizationId.set(this.user()?.organization?.id ?? '');
      return;
    }

    this.switchingOrganization.set(true);
    this.authService.switchOrganization(organizationId).subscribe({
      next: () => {
        this.selectedOrganizationId.set(organizationId);
        this.switchingOrganization.set(false);
        this.toastService.success(this.languageService.instant('organizationSwitcher.messages.switched'));
        this.loadOrganizationOptions();
        this.router.navigateByUrl('/dashboard');
      },
      error: (error: unknown) => {
        this.switchingOrganization.set(false);
        this.selectedOrganizationId.set(this.user()?.organization?.id ?? '');
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('organizationSwitcher.messages.switchError')));
      }
    });
  }

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

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
