import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { DashboardAnalyticsResponse } from '../models/dashboard-analytics.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardAnalyticsService {
  constructor(private readonly apiService: ApiService) {
  }

  loadAnalytics(months = 6, upcomingDays = 30, topLimit = 5): Observable<DashboardAnalyticsResponse> {
    return this.apiService.get<DashboardAnalyticsResponse>('/dashboard/analytics', {
      months,
      upcomingDays,
      topLimit
    });
  }
}
