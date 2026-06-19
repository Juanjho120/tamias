import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import { ProductBoxInventoryItemOption } from '../models/product-box-model.model';

@Injectable({ providedIn: 'root' })
export class ProductBoxReferenceDataService {
  constructor(private readonly apiService: ApiService) {}

  loadInventoryItems(): Observable<ProductBoxInventoryItemOption[]> {
    return this.apiService
      .get<PageResponse<ProductBoxInventoryItemOption>>('/inventory-items', {
        status: 'ACTIVE',
        page: 0,
        size: 500,
        sort: 'name,asc'
      })
      .pipe(map((response) => response.content));
  }
}
