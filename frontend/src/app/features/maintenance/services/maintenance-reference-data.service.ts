import { Injectable } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import { MaintenancePersonOption, MaintenanceReferenceOption, PropertyOption } from '../models/maintenance-reference.model';

export interface MaintenanceReferenceData {
  properties: PropertyOption[];
  categories: MaintenanceReferenceOption[];
  types: MaintenanceReferenceOption[];
  people: MaintenancePersonOption[];
}

@Injectable({
  providedIn: 'root'
})
export class MaintenanceReferenceDataService {
  constructor(private readonly apiService: ApiService) {
  }

  loadAll(): Observable<MaintenanceReferenceData> {
    return forkJoin({
      properties: this.apiService.get<PageResponse<PropertyOption>>('/properties', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      categories: this.apiService.get<PageResponse<MaintenanceReferenceOption>>('/catalogs/maintenance-categories', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      types: this.apiService.get<PageResponse<MaintenanceReferenceOption>>('/catalogs/maintenance-types', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      people: this.apiService.get<PageResponse<MaintenancePersonOption>>('/catalogs/maintenance-people', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'fullName,asc'
      }).pipe(map((response) => response.content))
    });
  }
}
