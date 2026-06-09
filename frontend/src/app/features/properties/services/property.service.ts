import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { PageResponse } from '../../../core/models/page-response.model';
import { Property, PropertyFilters, PropertyRequest, PropertySummary } from '../models/property.model';

@Injectable({
  providedIn: 'root'
})
export class PropertyService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(filters: PropertyFilters): Observable<PageResponse<PropertySummary>> {
    return this.apiService.get<PageResponse<PropertySummary>>('/properties', {
      status: filters.status,
      search: filters.search,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'createdAt,desc'
    });
  }

  findById(id: string): Observable<Property> {
    return this.apiService.get<Property>(`/properties/${id}`);
  }

  create(request: PropertyRequest): Observable<Property> {
    return this.apiService.post<Property>('/properties', request);
  }

  update(id: string, request: PropertyRequest): Observable<Property> {
    return this.apiService.put<Property>(`/properties/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/properties/${id}`);
  }
}
