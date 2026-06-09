import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  MaintenanceRecord,
  MaintenanceRecordFilters,
  MaintenanceRecordRequest,
  MaintenanceRecordSummary
} from '../models/maintenance-record.model';

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

  findById(id: string): Observable<MaintenanceRecord> {
    return this.apiService.get<MaintenanceRecord>(`/maintenance-records/${id}`);
  }

  create(request: MaintenanceRecordRequest): Observable<MaintenanceRecord> {
    return this.apiService.post<MaintenanceRecord>('/maintenance-records', request);
  }

  update(id: string, request: MaintenanceRecordRequest): Observable<MaintenanceRecord> {
    return this.apiService.put<MaintenanceRecord>(`/maintenance-records/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/maintenance-records/${id}`);
  }
}
