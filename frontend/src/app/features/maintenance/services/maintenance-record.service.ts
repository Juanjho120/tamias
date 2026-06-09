import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import { MaintenanceRecordFilters, MaintenanceRecordSummary } from '../models/maintenance-record.model';

@Injectable({
  providedIn: 'root'
})
export class MaintenanceRecordService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(filters: MaintenanceRecordFilters): Observable<PageResponse<MaintenanceRecordSummary>> {
    return this.apiService.get<PageResponse<MaintenanceRecordSummary>>('/maintenance-records', {
      propertyId: filters.propertyId,
      status: filters.status,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'createdAt,desc'
    });
  }
}
