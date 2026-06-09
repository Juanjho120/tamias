import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  TaskItem,
  TaskItemCompletionRequest,
  TaskItemRequest,
  TaskItemUpdateRequest,
  TaskList,
  TaskListFilters,
  TaskListRequest,
  TaskListSummary
} from '../models/task-list.model';

@Injectable({
  providedIn: 'root'
})
export class TaskListService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(filters: TaskListFilters): Observable<PageResponse<TaskListSummary>> {
    return this.apiService.get<PageResponse<TaskListSummary>>('/task-lists', {
      propertyId: filters.propertyId,
      reservationId: filters.reservationId,
      maintenanceRecordId: filters.maintenanceRecordId,
      status: filters.status,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'createdAt,desc'
    });
  }

  findById(id: string): Observable<TaskList> {
    return this.apiService.get<TaskList>(`/task-lists/${id}`);
  }

  create(request: TaskListRequest): Observable<TaskList> {
    return this.apiService.post<TaskList>('/task-lists', request);
  }

  update(id: string, request: TaskListRequest): Observable<TaskList> {
    return this.apiService.put<TaskList>(`/task-lists/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/task-lists/${id}`);
  }

  createItem(taskListId: string, request: TaskItemRequest): Observable<TaskItem> {
    return this.apiService.post<TaskItem>(`/task-lists/${taskListId}/items`, request);
  }

  updateItem(taskListId: string, itemId: string, request: TaskItemUpdateRequest): Observable<TaskItem> {
    return this.apiService.put<TaskItem>(`/task-lists/${taskListId}/items/${itemId}`, request);
  }

  updateItemCompletion(taskListId: string, itemId: string, request: TaskItemCompletionRequest): Observable<TaskItem> {
    return this.apiService.patch<TaskItem>(`/task-lists/${taskListId}/items/${itemId}/completion`, request);
  }

  deleteItem(taskListId: string, itemId: string): Observable<void> {
    return this.apiService.delete<void>(`/task-lists/${taskListId}/items/${itemId}`);
  }
}
