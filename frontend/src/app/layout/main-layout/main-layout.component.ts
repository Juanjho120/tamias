import { NgClass } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastContainerComponent } from '../../shared/toast/toast-container.component';

interface MenuItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [NgClass, RouterOutlet, RouterLink, RouterLinkActive, ToastContainerComponent],
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
    { label: 'Dashboard', icon: 'bi-speedometer2', route: '/dashboard' },
    { label: 'Properties', icon: 'bi-houses', route: '/properties' },
    { label: 'Catalogs', icon: 'bi-tags', route: '/catalogs' },
    { label: 'Maintenance', icon: 'bi-tools', route: '/maintenance' },
    { label: 'Scheduled Maintenance', icon: 'bi-calendar-check', route: '/scheduled-maintenance' },
    { label: 'Reservations', icon: 'bi-calendar2-week', route: '/reservations' },
    { label: 'Tasks', icon: 'bi-check2-square', route: '/tasks' },
    { label: 'Purchases', icon: 'bi-cart-check', route: '/purchases' },
    { label: 'Documents', icon: 'bi-file-earmark-text', route: '/documents' },
    { label: 'AI Assistant', icon: 'bi-stars', route: '/ai-assistant' }
  ];

  logout(): void {
    this.authService.logout();
  }
}
