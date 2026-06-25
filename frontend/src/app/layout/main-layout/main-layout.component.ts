import { NgClass } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { LanguageService } from '../../core/i18n/language.service';
import { ApiError } from '../../core/models/api-error.model';
import { AuthOrganizationOption } from '../../core/models/auth.models';
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
      :host { display: block; min-height: 100vh; overflow-x: hidden; }
      .app-shell { min-width: 0; overflow-x: hidden; }
      .sidebar { flex: 0 0 var(--tamias-sidebar-width); width: var(--tamias-sidebar-width); min-width: var(--tamias-sidebar-width); max-width: var(--tamias-sidebar-width); overflow-x: hidden; }
      .main-content { flex: 1 1 auto; min-width: 0; max-width: calc(100vw - var(--tamias-sidebar-width)); overflow-x: hidden; }
      main { min-width: 0; overflow-x: hidden; }
      :host ::ng-deep .page-card, :host ::ng-deep .card, :host ::ng-deep .table-responsive { min-width: 0; max-width: 100%; }
      :host ::ng-deep .table-responsive { overflow-x: auto; }
      :host ::ng-deep .ai-chat-card { min-height: calc(100vh - 14.5rem); }
      :host ::ng-deep .ai-session-list { max-height: calc(100vh - 25rem); }
      :host ::ng-deep .ai-messages { height: calc(100vh - 28rem); min-height: 320px; }
      :host ::ng-deep .ai-message-bubble, :host ::ng-deep .ai-message-content, :host ::ng-deep .ai-source-content { overflow-wrap: anywhere; word-break: break-word; }
      @media (max-width: 991.98px) {
        .main-content { max-width: 100vw; }
        :host ::ng-deep .ai-chat-card { min-height: auto; }
        :host ::ng-deep .ai-session-list, :host ::ng-deep .ai-messages { max-height: none; height: auto; }
      }
      .organization-logo, .organization-logo-fallback { width: 34px; height: 34px; min-width: 34px; border-radius: 0.6rem; }
      .organization-logo { object-fit: cover; border: 1px solid rgba(0, 0, 0, 0.08); background: #fff; }
      .organization-logo-fallback { display: inline-flex; align-items: center; justify-content: center; border: 1px solid rgba(13, 110, 253, 0.18); background: rgba(13, 110, 253, 0.08); color: #0d6efd; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.02em; text-transform: uppercase; }
      .organization-context { max-width: min(520px, 52vw); }
      .organization-switcher { min-width: 180px; max-width: 260px; }
      .organization-switcher .form-select { min-height: 31px; padding-top: 0.2rem; padding-bottom: 0.2rem; font-size: 0.8125rem; }
      .header-actions { flex-shrink: 0; }
    `
  ]
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private organizationOptionsRefreshSubscription?: Subscription;

  readonly user = this.authService.currentUser;
  readonly organizationOptions = signal<AuthOrganizationOption[]>([]);
  readonly loadingOrganizations = signal(false);
  readonly switchingOrganization = signal(false);
  readonly selectedOrganizationId = signal('');
  readonly currentOrganizationId = computed(() => this.user()?.organization?.id ?? '');
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
  readonly organizationSwitcherDisabled = computed(() => this.loadingOrganizations() || this.switchingOrganization());
  readonly menuItems = computed<MenuItem[]>(() => {
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
      { labelKey: 'navigation.payments', icon: 'bi-credit-card', route: '/payments' },
      { labelKey: 'navigation.documents', icon: 'bi-file-earmark-text', route: '/documents' },
      { labelKey: 'navigation.aiAssistant', icon: '', route: '/ai-assistant' }
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
    this.syncSelectedOrganizationFromSession();
    this.loadOrganizationOptions();
    this.organizationOptionsRefreshSubscription = this.authService.organizationOptionsRefresh$.subscribe(() => {
      this.loadOrganizationOptions();
    });
  }

  ngOnDestroy(): void {
    this.organizationOptionsRefreshSubscription?.unsubscribe();
  }

  loadOrganizationOptions(): void {
    if (!this.user()) {
      return;
    }

    this.loadingOrganizations.set(true);
    this.authService.listOrganizations().subscribe({
      next: (organizations) => {
        this.organizationOptions.set(organizations);
        this.syncSelectedOrganizationFromOptions(organizations);
        this.loadingOrganizations.set(false);
      },
      error: (error: unknown) => {
        this.loadingOrganizations.set(false);
        this.syncSelectedOrganizationFromSession();
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('organizationSwitcher.messages.loadError'))
        );
      }
    });
  }

  switchOrganization(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const organizationId = select.value;

    if (!organizationId || organizationId === this.currentOrganizationId() || this.switchingOrganization()) {
      this.syncSelectedOrganizationFromSession();
      return;
    }

    this.selectedOrganizationId.set(organizationId);
    this.switchingOrganization.set(true);
    this.authService.switchOrganization(organizationId).subscribe({
      next: () => {
        this.syncSelectedOrganizationFromSession();
        this.switchingOrganization.set(false);
        this.toastService.success(this.languageService.instant('organizationSwitcher.messages.switched'));
        this.loadOrganizationOptions();
        this.navigateAfterOrganizationSwitch();
      },
      error: (error: unknown) => {
        this.switchingOrganization.set(false);
        this.syncSelectedOrganizationFromSession();
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('organizationSwitcher.messages.switchError'))
        );
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

  private navigateAfterOrganizationSwitch(): void {
    const dashboardRoute = '/dashboard';
    const currentPath = this.router.url.split('?')[0];
    if (currentPath === dashboardRoute) {
      this.router.navigateByUrl('/profile', { skipLocationChange: true }).then(() => {
        this.router.navigateByUrl(dashboardRoute);
      });
      return;
    }

    this.router.navigateByUrl(dashboardRoute);
  }

  private syncSelectedOrganizationFromSession(): void {
    this.selectedOrganizationId.set(this.currentOrganizationId());
  }

  private syncSelectedOrganizationFromOptions(organizations: AuthOrganizationOption[]): void {
    const sessionOrganizationId = this.currentOrganizationId();
    const currentOrganization = organizations.find((organization) => organization.id === sessionOrganizationId)
      ?? organizations.find((organization) => organization.current);

    this.selectedOrganizationId.set(currentOrganization?.id ?? sessionOrganizationId);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
