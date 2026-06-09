import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import { CatalogFilters, CatalogItem, CatalogRequest } from '../models/catalog.model';

@Injectable({
  providedIn: 'root'
})
export class CatalogService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(endpoint: string, filters: CatalogFilters): Observable<PageResponse<CatalogItem>> {
    return this.apiService.get<PageResponse<CatalogItem>>(endpoint, {
      status: filters.status,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'createdAt,desc'
    });
  }

  findById(endpoint: string, id: string): Observable<CatalogItem> {
    return this.apiService.get<CatalogItem>(`${endpoint}/${id}`);
  }

  create(endpoint: string, request: CatalogRequest): Observable<CatalogItem> {
    return this.apiService.post<CatalogItem>(endpoint, request);
  }

  update(endpoint: string, id: string, request: CatalogRequest): Observable<CatalogItem> {
    return this.apiService.put<CatalogItem>(`${endpoint}/${id}`, request);
  }

  delete(endpoint: string, id: string): Observable<void> {
    return this.apiService.delete<void>(`${endpoint}/${id}`);
  }
}
