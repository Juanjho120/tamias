import { Injectable } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  ScheduledMaintenancePersonOption,
  ScheduledMaintenancePropertyOption,
  ScheduledMaintenanceReferenceOption
} from '../models/scheduled-maintenance-reference.model';

export interface ScheduledMaintenanceReferenceData {
  properties: ScheduledMaintenancePropertyOption[];
  categories: ScheduledMaintenanceReferenceOption[];
  types: ScheduledMaintenanceReferenceOption[];
  people: ScheduledMaintenancePersonOption[];
}

@Injectable({
  providedIn: 'root'
})
export class ScheduledMaintenanceReferenceDataService {
  constructor(private readonly apiService: ApiService) {
  }

  loadAll(): Observable<ScheduledMaintenanceReferenceData> {
    return forkJoin({
      properties: this.apiService.get<PageResponse<ScheduledMaintenancePropertyOption>>('/properties', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      categories: this.apiService.get<PageResponse<ScheduledMaintenanceReferenceOption>>('/catalogs/maintenance-categories', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      types: this.apiService.get<PageResponse<ScheduledMaintenanceReferenceOption>>('/catalogs/maintenance-types', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      people: this.apiService.get<PageResponse<ScheduledMaintenancePersonOption>>('/catalogs/maintenance-people', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'fullName,asc'
      }).pipe(map((response) => response.content))
    });
  }
}
