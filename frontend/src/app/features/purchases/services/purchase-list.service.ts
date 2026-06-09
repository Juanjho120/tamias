import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  PurchaseItem,
  PurchaseItemPurchasedRequest,
  PurchaseItemRequest,
  PurchaseItemUpdateRequest,
  PurchaseList,
  PurchaseListFilters,
  PurchaseListRequest,
  PurchaseListSummary
} from '../models/purchase-list.model';

@Injectable({
  providedIn: 'root'
})
export class PurchaseListService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(filters: PurchaseListFilters): Observable<PageResponse<PurchaseListSummary>> {
    return this.apiService.get<PageResponse<PurchaseListSummary>>('/purchase-lists', {
      propertyId: filters.propertyId,
      supplierId: filters.supplierId,
      cityId: filters.cityId,
      status: filters.status,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'purchaseDate,desc'
    });
  }

  findById(id: string): Observable<PurchaseList> {
    return this.apiService.get<PurchaseList>(`/purchase-lists/${id}`);
  }

  create(request: PurchaseListRequest): Observable<PurchaseList> {
    return this.apiService.post<PurchaseList>('/purchase-lists', request);
  }

  update(id: string, request: PurchaseListRequest): Observable<PurchaseList> {
    return this.apiService.put<PurchaseList>(`/purchase-lists/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/purchase-lists/${id}`);
  }

  createItem(purchaseListId: string, request: PurchaseItemRequest): Observable<PurchaseItem> {
    return this.apiService.post<PurchaseItem>(`/purchase-lists/${purchaseListId}/items`, request);
  }

  updateItem(purchaseListId: string, itemId: string, request: PurchaseItemUpdateRequest): Observable<PurchaseItem> {
    return this.apiService.put<PurchaseItem>(`/purchase-lists/${purchaseListId}/items/${itemId}`, request);
  }

  updateItemPurchased(purchaseListId: string, itemId: string, request: PurchaseItemPurchasedRequest): Observable<PurchaseItem> {
    return this.apiService.patch<PurchaseItem>(`/purchase-lists/${purchaseListId}/items/${itemId}/purchased`, request);
  }

  deleteItem(purchaseListId: string, itemId: string): Observable<void> {
    return this.apiService.delete<void>(`/purchase-lists/${purchaseListId}/items/${itemId}`);
  }
}
