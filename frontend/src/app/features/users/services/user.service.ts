import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  User,
  UserCreateRequest,
  UserFilters,
  UserSummary,
  UserUpdateRequest
} from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(filters: UserFilters): Observable<PageResponse<UserSummary>> {
    return this.apiService.get<PageResponse<UserSummary>>('/users', {
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'createdAt,desc'
    });
  }

  findById(id: string): Observable<User> {
    return this.apiService.get<User>(`/users/${id}`);
  }

  create(request: UserCreateRequest): Observable<User> {
    return this.apiService.post<User>('/users', request);
  }

  update(id: string, request: UserUpdateRequest): Observable<User> {
    return this.apiService.put<User>(`/users/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/users/${id}`);
  }
}
