import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface DashboardCard {
  title: string;
  description: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  readonly cards: DashboardCard[] = [
    {
      title: 'Properties',
      description: 'Manage houses, apartments, bungalows and villas.',
      icon: 'bi-houses',
      route: '/properties'
    },
    {
      title: 'Maintenance',
      description: 'Track maintenance records, materials, people and evidence.',
      icon: 'bi-tools',
      route: '/maintenance'
    },
    {
      title: 'Reservations',
      description: 'Manage guests, reservations and operational tasks.',
      icon: 'bi-calendar2-week',
      route: '/reservations'
    },
    {
      title: 'Documents',
      description: 'Upload important documents and index them for AI search.',
      icon: 'bi-file-earmark-text',
      route: '/documents'
    },
    {
      title: 'AI Assistant',
      description: 'Ask questions about indexed documents with source citations.',
      icon: 'bi-stars',
      route: '/ai-assistant'
    }
  ];
}
