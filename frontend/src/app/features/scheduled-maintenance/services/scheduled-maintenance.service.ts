import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  ScheduledMaintenance,
  ScheduledMaintenanceFilters,
  ScheduledMaintenanceHistory,
  ScheduledMaintenanceRequest,
  ScheduledMaintenanceRescheduleRequest,
  ScheduledMaintenanceStatusChangeRequest,
  ScheduledMaintenanceSummary
} from '../models/scheduled-maintenance.model';

@Injectable({
  providedIn: 'root'
})
export class ScheduledMaintenanceService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(filters: ScheduledMaintenanceFilters): Observable<PageResponse<ScheduledMaintenanceSummary>> {
    return this.apiService.get<PageResponse<ScheduledMaintenanceSummary>>('/scheduled-maintenance', {
      propertyId: filters.propertyId,
      status: filters.status,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'nextDueDate,asc'
    });
  }

  findDue(dueDate: string | null, page: number, size: number): Observable<PageResponse<ScheduledMaintenanceSummary>> {
    return this.apiService.get<PageResponse<ScheduledMaintenanceSummary>>('/scheduled-maintenance/due', {
      dueDate: dueDate || undefined,
      page,
      size,
      sort: 'nextDueDate,asc'
    });
  }

  findById(id: string): Observable<ScheduledMaintenance> {
    return this.apiService.get<ScheduledMaintenance>(`/scheduled-maintenance/${id}`);
  }

  findHistory(id: string): Observable<ScheduledMaintenanceHistory[]> {
    return this.apiService.get<ScheduledMaintenanceHistory[]>(`/scheduled-maintenance/${id}/history`);
  }

  create(request: ScheduledMaintenanceRequest): Observable<ScheduledMaintenance> {
    return this.apiService.post<ScheduledMaintenance>('/scheduled-maintenance', request);
  }

  update(id: string, request: ScheduledMaintenanceRequest): Observable<ScheduledMaintenance> {
    return this.apiService.put<ScheduledMaintenance>(`/scheduled-maintenance/${id}`, request);
  }

  reschedule(id: string, request: ScheduledMaintenanceRescheduleRequest): Observable<ScheduledMaintenance> {
    return this.apiService.patch<ScheduledMaintenance>(`/scheduled-maintenance/${id}/reschedule`, request);
  }

  pause(id: string, request: ScheduledMaintenanceStatusChangeRequest): Observable<ScheduledMaintenance> {
    return this.apiService.patch<ScheduledMaintenance>(`/scheduled-maintenance/${id}/pause`, request);
  }

  resume(id: string, request: ScheduledMaintenanceStatusChangeRequest): Observable<ScheduledMaintenance> {
    return this.apiService.patch<ScheduledMaintenance>(`/scheduled-maintenance/${id}/resume`, request);
  }

  cancel(id: string, request: ScheduledMaintenanceStatusChangeRequest): Observable<ScheduledMaintenance> {
    return this.apiService.patch<ScheduledMaintenance>(`/scheduled-maintenance/${id}/cancel`, request);
  }

  generateMaintenanceRecord(id: string): Observable<ScheduledMaintenance> {
    return this.apiService.post<ScheduledMaintenance>(`/scheduled-maintenance/${id}/generate-record`, {});
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/scheduled-maintenance/${id}`);
  }
}
