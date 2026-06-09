import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

interface DashboardCard {
  titleKey: string;
  descriptionKey: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  readonly cards: DashboardCard[] = [
    {
      titleKey: 'dashboard.cards.properties.title',
      descriptionKey: 'dashboard.cards.properties.description',
      icon: 'bi-houses',
      route: '/properties'
    },
    {
      titleKey: 'dashboard.cards.maintenance.title',
      descriptionKey: 'dashboard.cards.maintenance.description',
      icon: 'bi-tools',
      route: '/maintenance'
    },
    {
      titleKey: 'dashboard.cards.reservations.title',
      descriptionKey: 'dashboard.cards.reservations.description',
      icon: 'bi-calendar2-week',
      route: '/reservations'
    },
    {
      titleKey: 'dashboard.cards.documents.title',
      descriptionKey: 'dashboard.cards.documents.description',
      icon: 'bi-file-earmark-text',
      route: '/documents'
    },
    {
      titleKey: 'dashboard.cards.aiAssistant.title',
      descriptionKey: 'dashboard.cards.aiAssistant.description',
      icon: 'bi-stars',
      route: '/ai-assistant'
    }
  ];
}
