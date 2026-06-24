import { Injectable } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import { PaymentCategoryOption, PaymentPropertyOption } from '../models/payment-reference.model';

export interface PaymentReferenceData {
  properties: PaymentPropertyOption[];
  categories: PaymentCategoryOption[];
}

@Injectable({ providedIn: 'root' })
export class PaymentReferenceDataService {
  constructor(private readonly apiService: ApiService) {}

  loadAll(): Observable<PaymentReferenceData> {
    return forkJoin({
      properties: this.apiService
        .get<PageResponse<PaymentPropertyOption>>('/properties', {
          status: 'ACTIVE',
          page: 0,
          size: 200,
          sort: 'name,asc'
        })
        .pipe(map((response) => response.content)),
      categories: this.apiService
        .get<PageResponse<PaymentCategoryOption>>('/catalogs/payment-categories', {
          status: 'ACTIVE',
          page: 0,
          size: 200,
          sort: 'name,asc'
        })
        .pipe(map((response) => response.content))
    });
  }
}
