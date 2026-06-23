import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  Organization,
  OrganizationCreateRequest,
  OrganizationFilters,
  OrganizationStatus,
  OrganizationUpdateRequest
} from '../models/organization.model';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  constructor(private readonly apiService: ApiService) { }

  findAll(filters: OrganizationFilters): Observable<PageResponse<Organization>> {
    return this.apiService.get<PageResponse<Organization>>('/organizations', {
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'createdAt,desc'
    });
  }

  findById(id: string): Observable<Organization> {
    return this.apiService.get<Organization>(`/organizations/${id}`);
  }

  create(request: OrganizationCreateRequest): Observable<Organization> {
    return this.apiService.post<Organization>('/organizations', request);
  }

  update(id: string, request: OrganizationUpdateRequest): Observable<Organization> {
    return this.apiService.put<Organization>(`/organizations/${id}`, request);
  }

  updateStatus(id: string, status: OrganizationStatus): Observable<Organization> {
    return this.apiService.patch<Organization>(`/organizations/${id}/status`, { status });
  }

  uploadLogo(id: string, file: File): Observable<Organization> {
    const formData = new FormData();
    formData.append('file', file);
    return this.apiService.post<Organization>(`/organizations/${id}/logo`, formData);
  }

  deleteLogo(id: string): Observable<Organization> {
    return this.apiService.delete<Organization>(`/organizations/${id}/logo`);
  }
}
