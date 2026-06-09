import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/main-layout/main-layout.component').then((m) => m.MainLayoutComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        title: 'Dashboard | TAMIAS'
      },
      {
        path: 'properties',
        loadComponent: () => import('./features/properties/pages/properties-page/properties-page.component').then((m) => m.PropertiesPageComponent),
        title: 'Properties | TAMIAS'
      },
      {
        path: 'catalogs',
        loadComponent: () => import('./features/catalogs/pages/catalogs-page/catalogs-page.component').then((m) => m.CatalogsPageComponent),
        title: 'Catalogs | TAMIAS'
      },
      {
        path: 'maintenance',
        loadComponent: () => import('./features/maintenance/pages/maintenance-page/maintenance-page.component').then((m) => m.MaintenancePageComponent),
        title: 'Maintenance | TAMIAS'
      },
      {
        path: 'scheduled-maintenance',
        loadComponent: () => import('./features/scheduled-maintenance/pages/scheduled-maintenance-page/scheduled-maintenance-page.component').then((m) => m.ScheduledMaintenancePageComponent),
        title: 'Scheduled Maintenance | TAMIAS'
      },
      {
        path: 'reservations',
        loadComponent: () => import('./features/reservations/pages/reservations-page/reservations-page.component').then((m) => m.ReservationsPageComponent),
        title: 'Reservations | TAMIAS'
      },
      {
        path: 'tasks',
        loadComponent: () => import('./features/tasks/pages/tasks-page/tasks-page.component').then((m) => m.TasksPageComponent),
        title: 'Tasks | TAMIAS'
      },
      {
        path: 'purchases',
        loadComponent: () => import('./features/purchases/pages/purchases-page/purchases-page.component').then((m) => m.PurchasesPageComponent),
        title: 'Purchases | TAMIAS'
      },
      {
        path: 'documents',
        loadComponent: () => import('./features/placeholders/documents-page.component').then((m) => m.DocumentsPageComponent),
        title: 'Documents | TAMIAS'
      },
      {
        path: 'ai-assistant',
        loadComponent: () => import('./features/placeholders/ai-assistant-page.component').then((m) => m.AiAssistantPageComponent),
        title: 'AI Assistant | TAMIAS'
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
