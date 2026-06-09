import { Injectable } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  TaskMaintenanceRecordOption,
  TaskPropertyOption,
  TaskReservationOption,
  TaskTemplateOption
} from '../models/task-reference.model';

export interface TaskReferenceData {
  properties: TaskPropertyOption[];
  reservations: TaskReservationOption[];
  maintenanceRecords: TaskMaintenanceRecordOption[];
  taskTemplates: TaskTemplateOption[];
}

@Injectable({
  providedIn: 'root'
})
export class TaskReferenceDataService {
  constructor(private readonly apiService: ApiService) {
  }

  loadAll(): Observable<TaskReferenceData> {
    return forkJoin({
      properties: this.apiService.get<PageResponse<TaskPropertyOption>>('/properties', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      reservations: this.apiService.get<PageResponse<TaskReservationOption>>('/reservations', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'checkIn,desc'
      }).pipe(map((response) => response.content)),
      maintenanceRecords: this.apiService.get<PageResponse<TaskMaintenanceRecordOption>>('/maintenance-records', {
        page: 0,
        size: 200,
        sort: 'createdAt,desc'
      }).pipe(map((response) => response.content)),
      taskTemplates: this.apiService.get<PageResponse<TaskTemplateOption>>('/catalogs/task-templates', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content))
    });
  }
}
