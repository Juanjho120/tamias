import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import { Payment, PaymentFilters, PaymentRequest } from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private readonly apiService: ApiService) {}

  findAll(filters: PaymentFilters): Observable<PageResponse<Payment>> {
    return this.apiService.get<PageResponse<Payment>>('/payments', {
      propertyId: filters.propertyId,
      categoryId: filters.categoryId,
      method: filters.method,
      dateFrom: filters.dateFrom,
      dateTo: filters.dateTo,
      search: filters.search,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'payDate,desc'
    });
  }

  findById(id: string): Observable<Payment> {
    return this.apiService.get<Payment>(`/payments/${id}`);
  }

  create(request: PaymentRequest): Observable<Payment> {
    return this.apiService.post<Payment>('/payments', request);
  }

  update(id: string, request: PaymentRequest): Observable<Payment> {
    return this.apiService.put<Payment>(`/payments/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/payments/${id}`);
  }
}
